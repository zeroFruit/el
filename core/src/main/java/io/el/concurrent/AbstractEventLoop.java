package io.el.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventLoop implements EventLoop {

  private final Executor executor;

  public AbstractEventLoop(Executor executor) {
    this.executor = executor;
  }

  protected abstract void run();

  @Override
  public Promise<?> submit(Runnable task) {
    Promise<Void> promise = new DefaultPromise<>(this, task);
    execute(promise);
    return promise;
  }

  @Override
  public <T> Promise<T> submit(Callable<T> task) {
    Promise<T> promise = new DefaultPromise<>(this, task);
    execute(promise);
    return promise;
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  protected Executor executor() {
    return executor;
  }

  enum State {
    NOT_STARTED(1),
    STARTED(2),
    SHUTTING_DOWN(3),
    SHUTDOWN(4),
    TERMINATED(5);

    int value;

    State(int value) {
      this.value = value;
    }
  }
}
