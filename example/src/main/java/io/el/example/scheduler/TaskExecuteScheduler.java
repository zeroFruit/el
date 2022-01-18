package io.el.example.scheduler;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskExecuteScheduler extends SingleThreadEventLoop {

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
        .numOfTasks(100)
        .start();
  }
}
