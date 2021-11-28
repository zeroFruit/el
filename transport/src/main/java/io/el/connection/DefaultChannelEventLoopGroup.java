package io.el.connection;

import io.el.concurrent.EventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DefaultChannelEventLoopGroup extends ChannelMultiThreadEventLoopGroup {

  public DefaultChannelEventLoopGroup(int nThreads) {
    // FIXME: extract out ThreadPerTaskExecutor
    this(nThreads, new ThreadPerTaskExecutor(Executors.defaultThreadFactory()));
  }

  protected DefaultChannelEventLoopGroup(int nThreads, Executor executor,
      Object... args) {
    super(nThreads, executor, args);
  }

  @Override
  protected EventLoop newChild(Executor executor, Object... args) throws Exception {
    return new DefaultChannelEventLoop(this, executor);
  }
}
