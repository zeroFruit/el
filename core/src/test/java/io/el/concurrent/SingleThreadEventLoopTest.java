package io.el.concurrent;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

      CountDownLatch latch = new CountDownLatch(2);

      assertTimeout(Duration.ofSeconds(1), () -> {
        TestRunnable task1 = new TestRunnable(latch);
        eventLoop.execute(task1);

        TestRunnable task2 = new TestRunnable(latch);
        eventLoop.execute(task2);

        latch.await();

        assertTrue(task1.ran.get());
        assertTrue(task2.ran.get());
      });
    }
  }

  private static final class TestRunnable implements Runnable {
    final AtomicBoolean ran = new AtomicBoolean();
    final CountDownLatch latch;

    TestRunnable(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void run() {
      latch.countDown();
      ran.set(true);
    }
  }
}
