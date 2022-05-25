package io.el.concurrent;

import io.el.internal.PriorityQueueNode;
import io.el.internal.Time;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ScheduledPromise<V> extends DefaultPromise<V> implements PriorityQueueNode, Delayed {

  private final long deadlineNanos;
  private int queueIndex = INDEX_NOT_IN_QUEUE;
  private long id;

  @SuppressWarnings("unchecked")
  public ScheduledPromise(EventLoop eventLoop, Runnable task, long deadlineNanos) {
    super(eventLoop, task);
    this.deadlineNanos = deadlineNanos;
  }

  public ScheduledPromise(EventLoop eventLoop, Callable<V> task, long deadlineNanos) {
    super(eventLoop, task);
    this.deadlineNanos = deadlineNanos;
  }

  static long deadlineNanos(long delay) {
    long deadlineNanos = Time.currentNanos() + delay;
    return deadlineNanos < 0 ? Long.MAX_VALUE : deadlineNanos;
  }

  public long deadlineNanos() {
    return deadlineNanos;
  }

  ScheduledPromise<V> setId(long id) {
    if (this.id == 0L) {
      this.id = id;
    }
    return this;
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
  public int compareTo(Delayed o) {
    if (this == o) {
      return 0;
    }
    ScheduledPromise<?> that = (ScheduledPromise<?>) o;
    long d = deadlineNanos() - that.deadlineNanos();
    if (d < 0) {
      return -1;
    } else if (d > 0) {
      return 1;
    } else if (this.id < that.id) {
      return -1;
    } else {
      return 1;
    }
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return deadlineNanos() - Time.currentNanos();
  }

  private SingleThreadEventLoop singleThreadEventLoop() {
    return (SingleThreadEventLoop) eventLoop();
  }
}
