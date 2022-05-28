package io.el.channel;

import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DefaultChannelEventLoop extends ChannelSingleThreadEventLoop {

  public DefaultChannelEventLoop(ChannelEventLoopGroup parent) {
    super(new ThreadPerTaskExecutor(Executors.defaultThreadFactory()), parent);
  }

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
