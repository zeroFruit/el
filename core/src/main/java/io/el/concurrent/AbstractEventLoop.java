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
  public <V> Promise<V> submit(Callable<V> task) {
    Promise<V> promise = new DefaultPromise<>(this, task);
    execute(promise);
    return promise;
  }

  @Override
  public <V> Future<V> submit(Runnable task, V result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> List<Future<V>> invokeAll(Collection<? extends Callable<V>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> List<Future<V>> invokeAll(
      Collection<? extends Callable<V>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> V invokeAny(Collection<? extends Callable<V>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> V invokeAny(Collection<? extends Callable<V>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
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
