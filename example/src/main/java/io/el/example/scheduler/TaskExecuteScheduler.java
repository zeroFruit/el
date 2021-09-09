package io.el.example.scheduler;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskExecuteScheduler extends SingleThreadEventLoop {
  private static final Logger LOGGER = LogManager.getLogger();

  public TaskExecuteScheduler(Executor executor) {
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
    TaskExecuteScheduler scheduler = new TaskExecuteScheduler(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    int MAX_DELAY = 100000;
    long start = System.currentTimeMillis();
    for (int i = 0; i < 10000; i += 1) {
      long delayMillis = ThreadLocalRandom.current().nextInt(0, MAX_DELAY + 1);

      scheduler.schedule(
          new SimpleTask(String.valueOf(i), delayMillis),
          delayMillis, TimeUnit.MILLISECONDS);
    }

    LOGGER.info("Thread[{}] - Executed all tasks - {} ms", Thread.currentThread().getName(),  System.currentTimeMillis() - start);
    Thread.sleep(MAX_DELAY + 2000);

    scheduler.shutdownGracefully(100, TimeUnit.MILLISECONDS);
    LOGGER.info("Thread[{}] - Shutdown scheduler gracefully", Thread.currentThread().getName());
  }
}
