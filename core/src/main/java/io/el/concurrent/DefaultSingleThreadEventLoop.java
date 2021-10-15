package io.el.concurrent;

import java.util.concurrent.Executor;

public class DefaultSingleThreadEventLoop extends SingleThreadEventLoop{

  public DefaultSingleThreadEventLoop(Executor executor) {
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
}
