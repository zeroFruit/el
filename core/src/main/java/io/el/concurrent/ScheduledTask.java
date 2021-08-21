package io.el.concurrent;

import io.el.internal.PriorityQueueNode;
import io.el.internal.Time;

public class ScheduledTask<V> extends DefaultTask<V> implements Runnable, PriorityQueueNode {
  static long deadlineNanos(long delay) {
    long deadlineNanos = Time.currentNanos() + delay;
    return deadlineNanos < 0 ? Long.MAX_VALUE : deadlineNanos;
  }

  final private long deadlineNanos;

  private Runnable task;

  private int queueIndex = INDEX_NOT_IN_QUEUE;
  private long id;

  public ScheduledTask(EventLoop eventLoop, Runnable task, long deadlineNanos) {
    super(eventLoop);
    this.task = task;
    this.deadlineNanos = deadlineNanos;
  }

  public long deadlineNanos() {
    return deadlineNanos;
  }

  ScheduledTask<V> setId(long id) {
    if (this.id == 0L) {
      this.id = id;
    }
    return this;
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public void prioritize(int i) {

  }

  @Override
  public int index() {
    return queueIndex;
  }

  @Override
  public void index(int i) {
    queueIndex = i;
  }

  @Override
  public void run() {
    if (!eventLoop().inEventLoop()) {
      return;
    }
    try {
      if (isCancelled()) {
        singleThreadEventLoop().scheduledTaskQueue().removeTyped(this);
        return;
      }
      task.run();
      setSuccess(null);
    } catch (Throwable cause) {
      setFailure(cause);
    }
  }

  private SingleThreadEventLoop singleThreadEventLoop() {
    return (SingleThreadEventLoop) eventLoop();
  }
}
