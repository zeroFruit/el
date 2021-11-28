package io.el.connection;

import java.util.concurrent.Executor;

public class DefaultChannelEventLoop extends ChannelSingleThreadEventLoop {

  public DefaultChannelEventLoop(ChannelEventLoopGroup parent,
      Executor executor) {
    super(parent, executor);
  }

  @Override
  protected void run() {
    while (true) {
      Runnable task = takeTask();
      if (task != null) {
        task.run();
      }
      if (confirmShutdown()) {
        break;
      }
    }
  }
}
