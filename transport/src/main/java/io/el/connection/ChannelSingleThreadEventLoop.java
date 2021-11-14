package io.el.connection;

import io.el.concurrent.EventLoop;
import io.el.concurrent.SingleThreadEventLoop;
import java.util.Queue;
import java.util.concurrent.Executor;

public abstract class ChannelSingleThreadEventLoop extends SingleThreadEventLoop
    implements ChannelEventLoop {

  protected static final int DEFAULT_MAX_PENDING_TASKS = 16;
  ChannelEventLoopGroup parent;
  private final Queue<Runnable> tailTasks;

  public ChannelSingleThreadEventLoop(ChannelEventLoopGroup parent,
      Executor executor, Queue<Runnable> tailTaskQueue) {
    super(executor);
    this.parent = parent;
    this.tailTasks = tailTaskQueue;
  }

  @Override
  public ChannelEventLoopGroup parent() {
    return parent;
  }

  @Override
  public EventLoop next() {
    return this;
  }

  @Override
  public ChannelPromise register(Channel channel) {
    ChannelPromise promise = new DefaultChannelPromise(channel, this);
    channel.register(this, promise);
    return promise;
  }

  protected boolean hasTasks() {
    return !tailTasks.isEmpty();
  }
}
