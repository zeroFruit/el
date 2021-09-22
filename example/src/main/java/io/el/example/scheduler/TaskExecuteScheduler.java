package io.el.example.scheduler;

import io.el.concurrent.AbstractEventLoop;
import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import io.el.example.AbstractSingleThreadEventLoopExample;
import io.el.example.SimpleTask;
import java.util.Date;
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

  private static class Example extends AbstractSingleThreadEventLoopExample {

    public Example(SingleThreadEventLoop eventLoop) {
      super(eventLoop);
    }

    @Override
    protected void testEventLoop(String taskId, long scheduledDelayMillis) {
      eventLoop().schedule(
          new SimpleTask(taskId, taskDelay(), scheduledDelayMillis),
          scheduledDelayMillis, TimeUnit.MILLISECONDS);
    }
  }

  public static void main(String[] args) throws InterruptedException {
    TaskExecuteScheduler scheduler = new TaskExecuteScheduler(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    new Example(scheduler)
        .maxDelay(500)
        .minDelay(0)
        .taskDelay(500)
        .numOfTasks(10)
        .start();
  }
}
