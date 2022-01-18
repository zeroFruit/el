package io.el.example.executor;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskExecutor extends SingleThreadEventLoop {

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

  private static class Example extends AbstractSingleThreadEventLoopExample {

    public Example(SingleThreadEventLoop eventLoop) {
      super(eventLoop);
    }

    @Override
    protected void testEventLoop(String taskId, long scheduledDelayMillis) {
      eventLoop().submit(new SimpleTask(taskId, taskDelay()));
    }
  }

  public static void main(String[] args) throws InterruptedException {
    TaskExecutor scheduler = new TaskExecutor(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    new Example(scheduler)
        .taskDelay(100)
        .numOfTasks(100)
        .intervalBetweenTasks(100)
        .start();
  }
}
