package io.el.channel;

import io.el.concurrent.EventLoop;
import io.el.concurrent.SingleThreadEventLoop;
import io.el.internal.ObjectUtil;
import java.util.Iterator;
import java.util.concurrent.Executor;

public abstract class ChannelSingleThreadEventLoop extends SingleThreadEventLoop
    implements ChannelEventLoop {

  private final ChannelEventLoopGroup parent;

  protected ChannelSingleThreadEventLoop(Executor executor, ChannelEventLoopGroup parent) {
    super(executor);
    this.parent = parent;
  }

  @Override
  public Iterator<EventLoop> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChannelEventLoopGroup parent() {
    return parent;
  }

  @Override
  public ChannelEventLoop next() {
    return parent.next();
  }

  @Override
  public ChannelPromise register(Channel channel) {
    ChannelPromise promise = new DefaultChannelPromise(channel, this);
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().internal().register(this, promise);
    return promise;
  }
}
