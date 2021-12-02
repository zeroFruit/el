package io.el.example.minimal;

import io.el.concurrent.SingleThreadEventLoop;
import java.util.concurrent.Executor;

/**
 * We need to create a new EventLoop class.
 * This event loop does not do any IO operation
 */
public class DefaultSingleThreadEventLoop extends SingleThreadEventLoop {
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
