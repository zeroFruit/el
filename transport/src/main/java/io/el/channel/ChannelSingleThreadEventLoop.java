package io.el.channel;

import io.el.concurrent.EventLoop;
import io.el.concurrent.SingleThreadEventLoop;
import io.el.internal.ObjectUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

public class ChannelSingleThreadEventLoop extends SingleThreadEventLoop implements
    ChannelEventLoop {

  private final Set<EventLoop> selfSet = Collections.singleton(this);
  private final ChannelEventLoopGroup parent;

  public ChannelSingleThreadEventLoop(
      Executor executor,
      ChannelEventLoopGroup parent
  ) {
    super(executor);
    this.parent = parent;
  }

  @Override
  protected void run() {
    do {
      Runnable task = takeTask();
      if (task != null) {
        task.run();
        updateLastExecutionTime();
      }
    } while (!confirmShutdown());
  }

  @Override
  public Iterator<EventLoop> iterator() {
    return selfSet.iterator();
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
    return register(new DefaultChannelPromise(channel, this));
  }

  @Override
  public ChannelPromise register(final ChannelPromise promise) {
    ObjectUtil.checkNotNull(promise, "promise");
    promise.channel().internal().register(this, promise);
    return promise;
  }
}
