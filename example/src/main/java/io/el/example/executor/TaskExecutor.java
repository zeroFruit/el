package io.el.example.executor;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskExecutor extends SingleThreadEventLoop {
  private static final Logger LOGGER = LogManager.getLogger();

  public TaskExecutor(Executor executor) {
    super(executor);
  }

  @Override
  protected void run() {
    while (!confirmShutdown()) {
      Runnable task = takeTask();
      if (task != null) {
        task.run();
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    TaskExecutor scheduler = new TaskExecutor(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    int MAX_DELAY = 10000;

    long start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i += 1) {
      long innerStart = System.currentTimeMillis();
      try {
        scheduler.execute(new SimpleTask(String.valueOf(i), 500));
      } catch (RejectedExecutionException e) {
        LOGGER.info("Thread[{}] - Executed failed. Task ID [{}] - {} ms", Thread.currentThread().getName(), i, System.currentTimeMillis() - innerStart);
      }
    }

    LOGGER.info("Thread[{}] - Executed all tasks - {} ms", Thread.currentThread().getName(), System.currentTimeMillis() - start);
    Thread.sleep(2 * MAX_DELAY);

    scheduler.shutdownGracefully(100, TimeUnit.MILLISECONDS);
    LOGGER.info("Thread[{}] - Shutdown scheduler gracefully", Thread.currentThread().getName());
  }
}
