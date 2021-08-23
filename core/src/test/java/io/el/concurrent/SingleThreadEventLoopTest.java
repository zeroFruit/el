package io.el.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class SingleThreadEventLoopTest {
  @Nested
  @DisplayName("On takeTask() method")
  class TakeTaskMethod {
    @Test
    @DisplayName("When eventloop is given tasks, then execute them")
    public void runTasks() {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      CountDownLatch latch = new CountDownLatch(3);

      assertTimeout(Duration.ofSeconds(1), () -> {
        TestTask task1 = new TestTask(latch);
        eventLoop.execute(task1);

        TestTask task2 = new TestTask(latch);
        eventLoop.schedule(task2, 200, TimeUnit.MILLISECONDS);

        TestTask task3 = new TestTask(latch);
        eventLoop.execute(task3);

        latch.await();

        assertEquals(task1.order, 1);
        assertEquals(task2.order, 3);
        assertEquals(task3.order, 2);
      });
    }

    @Test
    @DisplayName("When eventloop is given multiple scheduled tasks, then handle them in proper order")
    public void runScheduledTasks() {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      CountDownLatch latch = new CountDownLatch(3);

      assertTimeout(Duration.ofSeconds(1), () -> {
        TestTask task1 = new TestTask(latch);
        eventLoop.schedule(task1, 600, TimeUnit.MILLISECONDS);

        TestTask task2 = new TestTask(latch);
        eventLoop.schedule(task2, 200, TimeUnit.MILLISECONDS);

        TestTask task3 = new TestTask(latch);
        eventLoop.schedule(task3, 400, TimeUnit.MILLISECONDS);

        latch.await();

        assertEquals(task1.order, 3);
        assertEquals(task2.order, 1);
        assertEquals(task3.order, 2);
      });
    }
  }

  @Nested
  @DisplayName("On shutdownGracefully() method")
  class ShutdownGracefullyMethod {
    @Test
    @DisplayName("When eventloop shutdown gracefully and execute task, then throw exception")
    public void testAddTaskAfterShutdown() {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      CountDownLatch latch = new CountDownLatch(1);

      assertThrows(RejectedExecutionException.class, () -> {
        eventLoop.execute(new TestTask(latch));

        latch.await();

        assertTrue(eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS));

        eventLoop.execute(new TestTask());
      });
    }

    @Test
    @DisplayName("When eventloop shutdownGracefully, then before timeout, its state is not SHUTDOWN")
    public void testShutdown() throws InterruptedException {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      assertTrue(eventLoop.shutdownGracefully(200, TimeUnit.MILLISECONDS));

      Thread.sleep(100);

      assertFalse(eventLoop.isShutdown());
    }
    @Test
    @DisplayName("When eventloop shutdownGracefully, then remove all scheduled tasks")
    public void testRemoveAllScheduledTasks() throws InterruptedException {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      // Although task1 is scheduled to run after 100ms (which is < 200ms), event loop updates its
      // state into SHUTTING_DOWN right after calling shutdownGracefully() and remove all scheduled
      // tasks. So task1 is not running.
      TestTask task1 = new TestTask();
      ScheduledTask scheduledTask1 = eventLoop.schedule(task1, 100, TimeUnit.MILLISECONDS);

      TestTask task2 = new TestTask();
      ScheduledTask scheduledTask2 = eventLoop.schedule(task2, 300, TimeUnit.MILLISECONDS);

      assertTrue(eventLoop.shutdownGracefully(200, TimeUnit.MILLISECONDS));

      Thread.sleep(300);

      assertFalse(scheduledTask1.isDone());
      assertFalse(scheduledTask2.isDone());
      assertTrue(eventLoop.isShutdown());
    }

    @Test
    @DisplayName("When eventloop shutdownGracefully, then run existing tasks")
    public void testRunRemainingTasks2() throws InterruptedException {
      final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
          new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
      ) {
        @Override
        protected void run() {
          while (!confirmShutdown()) {
            Runnable task = takeTask();
            if (task != null) {
              task.run();
            }
          }
        }
      };

      TimeTakingTask task1 = new TimeTakingTask(50);
      eventLoop.execute(task1);

      // Although task2 has longer duration than timeout, it runs.
      TimeTakingTask task2 = new TimeTakingTask(200);
      eventLoop.execute(task2);

      assertTrue(eventLoop.shutdownGracefully(100, TimeUnit.MILLISECONDS));

      Thread.sleep(300);

      assertTrue(task1.ran);
      assertTrue(task2.ran);
      assertTrue(eventLoop.isShutdown());
    }
  }

  private static final class TestTask implements Runnable {
    static AtomicInteger ORDER = new AtomicInteger(0);

    final CountDownLatch latch;
    int order;

    TestTask() {
      this.latch = null;
    }

    TestTask(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void run() {
      order = ORDER.incrementAndGet();
      if (latch == null) {
        return;
      }
      latch.countDown();
    }
  }

  private static final class TimeTakingTask implements Runnable {
    static AtomicInteger ORDER = new AtomicInteger(0);

    long durationMillis;
    boolean ran;

    TimeTakingTask(long durationMillis) {
      this.durationMillis = durationMillis;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(durationMillis);
        ran = true;
      } catch (InterruptedException e) {
        // NO-OP
      }
    }
  }
}
