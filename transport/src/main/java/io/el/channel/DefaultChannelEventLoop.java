package io.el.channel;

import java.util.concurrent.Executor;

public class DefaultChannelEventLoop extends ChannelSingleThreadEventLoop {

  public DefaultChannelEventLoop(Executor executor, ChannelEventLoopGroup parent) {
    super(executor, parent);
  }

  @Override
  public void run() {
    do {
      Runnable task = takeTask();
      if (task != null) {
        task.run();
        updateLastExecutionTime();
      }
    } while (!confirmShutdown());
  }
}
