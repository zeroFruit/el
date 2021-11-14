package io.el.connection;

import io.el.concurrent.DefaultEventLoopChooserFactory;
import io.el.concurrent.EventLoop;
import io.el.concurrent.MultiThreadEventLoop;
import io.el.concurrent.Promise;
import java.util.concurrent.Executor;

public abstract class ChannelMultiThreadEventLoopGroup extends MultiThreadEventLoop
    implements ChannelEventLoopGroup {

  private static final int DEFAULT_EVENT_LOOP_THREADS = 1;

  protected ChannelMultiThreadEventLoopGroup(
      int nThreads, Executor executor, Object... args) {
    super(nThreads, executor, DefaultEventLoopChooserFactory.INSTANCE, args);
  }

  @Override
  protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

  @Override
  public EventLoop next() {
    return super.next();
  }

  @Override
  public ChannelPromise register(Channel channel) {
    return null;
  }

  @Override
  public Promise<?> shutdownGracefully() {
    return null;
  }
}
