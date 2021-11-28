package io.el.connection.nio;

import io.el.concurrent.EventLoop;
import io.el.connection.ChannelEventLoopTaskQueueFactory;
import io.el.connection.ChannelMultiThreadEventLoopGroup;
import io.el.connection.DefaultSelectStrategyFactory;
import io.el.connection.SelectStrategyFactory;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

public class NioChannelEventLoopGroup extends ChannelMultiThreadEventLoopGroup {

  public NioChannelEventLoopGroup() {
    this(0);
  }

  public NioChannelEventLoopGroup(int nThreads) {
    super(nThreads, null, SelectorProvider.provider(),
        DefaultSelectStrategyFactory.INSTANCE);
  }

  @Override
  protected EventLoop newChild(Executor executor, Object... args) throws Exception {
    SelectorProvider selectorProvider = (SelectorProvider) args[0];
    SelectStrategyFactory selectStrategyFactory = (SelectStrategyFactory) args[1];
    ChannelEventLoopTaskQueueFactory tailTaskQueueFactory = (ChannelEventLoopTaskQueueFactory) args[2];
    return new NioChannelEventLoop(this, executor, selectorProvider,
        selectStrategyFactory.newSelectStrategy(), tailTaskQueueFactory);
  }
}
