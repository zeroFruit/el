package io.el.example.scheduler;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import io.el.example.AbstractSingleThreadEventLoopExample;
import io.el.example.SimpleTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LongDelayTaskExecuteScheduler extends SingleThreadEventLoop {

  public LongDelayTaskExecuteScheduler(Executor executor) {
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
    LongDelayTaskExecuteScheduler scheduler = new LongDelayTaskExecuteScheduler(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    new Example(scheduler)
        .maxDelay(3000)
        .minDelay(1500)
        .taskDelay(500)
        .numOfTasks(10)
        .start();
  }
}
