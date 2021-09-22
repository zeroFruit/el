package io.el.example.executor;

import io.el.concurrent.SingleThreadEventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import io.el.example.AbstractSingleThreadEventLoopExample;
import io.el.example.SimpleTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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

  private static class Example extends AbstractSingleThreadEventLoopExample {

    public Example(SingleThreadEventLoop eventLoop) {
      super(eventLoop);
    }

    @Override
    protected void testEventLoop(String taskId, long scheduledDelayMillis) {
      eventLoop().execute(new SimpleTask(taskId, taskDelay()));
    }
  }

  public static void main(String[] args) throws InterruptedException {
    TaskExecutor scheduler = new TaskExecutor(
        new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
    new Example(scheduler)
        .taskDelay(500)
        .numOfTasks(10)
        .start();
  }
}
