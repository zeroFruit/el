package io.el.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Callable;
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

  private static final class TestTask implements Runnable {

    final AtomicInteger ORDER;

    final CountDownLatch LATCH;

    int order;

    TestTask(AtomicInteger ORDER) {
      this.LATCH = null;
      this.ORDER = ORDER;
    }

    TestTask(CountDownLatch LATCH, AtomicInteger ORDER) {
      this.LATCH = LATCH;
      this.ORDER = ORDER;
    }

    @Override
    public void run() {
      order = ORDER.incrementAndGet();
      if (LATCH == null) {
        return;
      }
      LATCH.countDown();
    }
  }

  private static final class TestCallableTask implements Callable<Integer> {

    final AtomicInteger ORDER;

    int order;
    int value;

    TestCallableTask(AtomicInteger ORDER, int value) {
      this.ORDER = ORDER;
      this.value = value;
    }

    @Override
    public Integer call() {
      order = ORDER.incrementAndGet();
      return value;
    }
  }

  private static final class TimeTakingTask implements Runnable {

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

  private static final class TimeTakingCallableTask implements Callable<Integer> {

    final AtomicInteger ORDER;

    long durationMillis;
    int value;
    int order;

    TimeTakingCallableTask(AtomicInteger ORDER, long durationMillis, int value) {
      this.ORDER = ORDER;
      this.durationMillis = durationMillis;
      this.value = value;
    }

    @Override
    public Integer call() {
      try {
        Thread.sleep(durationMillis);
        order = ORDER.incrementAndGet();
      } catch (InterruptedException e) {
        // NO-OP
      }
      return value;
    }
  }

  private final SingleThreadEventLoop eventLoop =
      new SingleThreadEventLoop(new ThreadPerTaskExecutor(Executors.defaultThreadFactory())) {
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

  @Nested
  @DisplayName("On takeTask() method")
  class TakePromiseMethod {

    @Test
    @DisplayName("When EventLoop is given tasks, then execute them")
    public void runTasks() {
      CountDownLatch LATCH = new CountDownLatch(3);
      AtomicInteger ORDER = new AtomicInteger(0);

      assertTimeout(
          Duration.ofSeconds(1),
          () -> {
            try {
              TestTask task1 = new TestTask(LATCH, ORDER);
              eventLoop.execute(task1);

              TestTask task2 = new TestTask(LATCH, ORDER);
              eventLoop.schedule(task2, 200, TimeUnit.MILLISECONDS);

              TestTask task3 = new TestTask(LATCH, ORDER);
              eventLoop.execute(task3);

              LATCH.await();

              assertEquals(task1.order, 1);
              assertEquals(task2.order, 3);
              assertEquals(task3.order, 2);
            } finally {
              eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
            }
          });
    }

    @Test
    @DisplayName(
        "When EventLoop is given multiple scheduled tasks, then handle them in proper order")
    public void runScheduledTasks() {
      CountDownLatch LATCH = new CountDownLatch(3);
      AtomicInteger ORDER = new AtomicInteger(0);

      assertTimeout(
          Duration.ofSeconds(1),
          () -> {
            try {
              TestTask task1 = new TestTask(LATCH, ORDER);
              eventLoop.schedule(task1, 600, TimeUnit.MILLISECONDS);

              TestTask task2 = new TestTask(LATCH, ORDER);
              eventLoop.schedule(task2, 200, TimeUnit.MILLISECONDS);

              TestTask task3 = new TestTask(LATCH, ORDER);
              eventLoop.schedule(task3, 400, TimeUnit.MILLISECONDS);

              LATCH.await();

              assertEquals(task1.order, 3);
              assertEquals(task2.order, 1);
              assertEquals(task3.order, 2);
            } finally {
              eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
            }
          });
    }

    @Test
    @DisplayName("When EventLoop is given callable task, then return Promise with result")
    public void runCallable() {
      AtomicInteger ORDER = new AtomicInteger(0);

      assertTimeout(
          Duration.ofSeconds(1),
          () -> {
            TestCallableTask task = new TestCallableTask(ORDER, 1);
            Promise<Integer> promise = eventLoop.submit(task).await();
            assertEquals(promise.get().intValue(), 1);
          });
    }

    @Test
    @DisplayName("When EventLoop schedules Callables, then return result in order")
    public void scheduleCallables() {
      AtomicInteger ORDER = new AtomicInteger(0);

      assertTimeout(
          Duration.ofSeconds(1),
          () -> {
            TestCallableTask task1 = new TestCallableTask(ORDER, 1);
            Promise<Integer> promise1 = eventLoop.schedule(task1, 400, TimeUnit.MILLISECONDS);

            TestCallableTask task2 = new TestCallableTask(ORDER, 2);
            Promise<Integer> promise2 = eventLoop.schedule(task2, 200, TimeUnit.MILLISECONDS);

            Thread.sleep(500);

            assertTrue(promise1.isSuccess());
            assertTrue(promise2.isSuccess());
            assertEquals(promise1.get().intValue(), 1);
            assertEquals(promise2.get().intValue(), 2);
            assertEquals(task1.order, 2);
            assertEquals(task2.order, 1);
          });
    }

    /**
     * `task1` and `task2` are scheduled with the same delay. In this case because `task1` is
     * scheduled first, it enqueues into TaskQueue before `task2`. And because
     * `SingleThreadEventLoop` executes task by single task, it waits until `task1` finish.
     */
    @Test
    @DisplayName("When EventLoop schedules Callables, then return result in order")
    public void scheduleTimeTakingCallables() {
      AtomicInteger ORDER = new AtomicInteger(0);

      assertTimeout(
          Duration.ofSeconds(1),
          () -> {
            TimeTakingCallableTask task1 = new TimeTakingCallableTask(ORDER, 400, 1);
            Promise<Integer> promise1 = eventLoop.schedule(task1, 100, TimeUnit.MILLISECONDS);

            TimeTakingCallableTask task2 = new TimeTakingCallableTask(ORDER, 200, 2);
            Promise<Integer> promise2 = eventLoop.schedule(task2, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(800);

            assertTrue(promise1.isSuccess());
            assertTrue(promise2.isSuccess());
            assertEquals(promise1.get().intValue(), 1);
            assertEquals(promise2.get().intValue(), 2);
            assertEquals(task1.order, 1);
            assertEquals(task2.order, 2);
          });
    }
  }

  @Nested
  @DisplayName("On shutdownGracefully() method")
  class ShutdownGracefullyMethod {

    @Test
    @DisplayName("When EventLoop shutdown gracefully and execute task, then throw exception")
    public void testAddTaskAfterShutdown() {
      CountDownLatch LATCH = new CountDownLatch(1);
      AtomicInteger ORDER = new AtomicInteger(0);

      assertThrows(
          RejectedExecutionException.class,
          () -> {
            try {
              eventLoop.execute(new TestTask(LATCH, ORDER));

              LATCH.await();

              assertTrue(eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS));

              eventLoop.execute(new TestTask(ORDER));
            } finally {
              eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
            }
          });
    }

    @Test
    @DisplayName(
        "When EventLoop shutdownGracefully, then before timeout, its state is not SHUTDOWN")
    public void testShutdown() throws InterruptedException {
      try {
        assertTrue(eventLoop.shutdownGracefully(200, TimeUnit.MILLISECONDS));

        Thread.sleep(100);

        assertFalse(eventLoop.isShutdown());
      } finally {
        eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
      }
    }

    @Test
    @DisplayName("When EventLoop shutdownGracefully, then remove all scheduled tasks")
    public void testRemoveAllScheduledTasks() throws InterruptedException {
      AtomicInteger ORDER = new AtomicInteger(0);

      try {
        // Although task1 is scheduled to run after 100ms (which is < 200ms), event loop updates its
        // state into SHUTTING_DOWN right after calling shutdownGracefully() and remove all
        // scheduled
        // tasks. So task1 is not running.
        TestTask task1 = new TestTask(ORDER);
        ScheduledPromise scheduledPromise1 = eventLoop.schedule(task1, 100, TimeUnit.MILLISECONDS);

        TestTask task2 = new TestTask(ORDER);
        ScheduledPromise scheduledPromise2 = eventLoop.schedule(task2, 300, TimeUnit.MILLISECONDS);

        assertTrue(eventLoop.shutdownGracefully(200, TimeUnit.MILLISECONDS));

        Thread.sleep(300);

        assertFalse(scheduledPromise1.isDone());
        assertFalse(scheduledPromise2.isDone());
        assertTrue(eventLoop.isShutdown());
      } finally {
        eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
      }
    }

    @Test
    @DisplayName("When EventLoop shutdownGracefully, then run existing tasks")
    public void testRunRemainingTasks2() throws InterruptedException {
      try {
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
      } finally {
        eventLoop.shutdownGracefully(0L, TimeUnit.MILLISECONDS);
      }
    }
  }
}
