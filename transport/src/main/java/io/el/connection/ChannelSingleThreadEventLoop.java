package io.el.connection;

import io.el.concurrent.EventLoop;
import io.el.concurrent.SingleThreadEventLoop;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ChannelSingleThreadEventLoop extends SingleThreadEventLoop
    implements ChannelEventLoop {

  protected static final int DEFAULT_MAX_PENDING_TASKS = 16;
  ChannelEventLoopGroup parent;
  private final Queue<Runnable> tailTasks;
  private final Collection<EventLoop> selfCollection = Collections.singleton(this);

  public ChannelSingleThreadEventLoop(ChannelEventLoopGroup parent,
      Executor executor) {
    super(executor);
    this.parent = parent;
    this.tailTasks = newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
  }

  public ChannelSingleThreadEventLoop(ChannelEventLoopGroup parent,
      Executor executor, Queue<Runnable> tailTaskQueue) {
    super(executor);
    this.parent = parent;
    this.tailTasks = tailTaskQueue;
  }

  @Override
  public Iterator<EventLoop> iterator() {
    return selfCollection.iterator();
  }

  @Override
  public ChannelEventLoopGroup parent() {
    return parent;
  }

  @Override
  public ChannelEventLoop next() {
    return this;
  }

  @Override
  public ChannelPromise register(Channel channel) {
    ChannelPromise promise = new DefaultChannelPromise(channel, this);
    channel.internal().register(this, promise);
    return promise;
  }

  protected boolean hasTasks() {
    return super.hasTasks() || !tailTasks.isEmpty();
  }

  protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
    return new LinkedBlockingQueue<>(maxPendingTasks);
  }
}
