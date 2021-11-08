package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static io.el.internal.ObjectUtil.checkPositiveOrZero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultPromise<V> implements Promise<V> {

  private static final Object EMPTY_RESULT = new Object();
  private static final Logger LOGGER = LogManager.getLogger();
  private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> resultUpdater =
      AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");
  private static final AtomicReferenceFieldUpdater<DefaultPromise, Throwable> causeUpdater =
      AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Throwable.class, "cause");

  private final EventLoop eventLoop;
  private final Callable<V> task;
  private volatile V result;
  private volatile Throwable cause;
  private List<PromiseListener> listeners = new ArrayList<>();

  @SuppressWarnings("unchecked")
  public DefaultPromise(EventLoop eventLoop, Runnable task) {
    this.eventLoop = eventLoop;
    this.task = (Callable<V>) Executors.callable(task);
  }

  public DefaultPromise(EventLoop eventLoop, Callable<V> task) {
    this.eventLoop = eventLoop;
    this.task = task;
  }

  protected DefaultPromise() {
    this.eventLoop = null;
    this.task = (Callable<V>) Executors.callable(() -> {});
  }

  @Override
  public void run() {
    if (!eventLoop().inEventLoop()) {
      return;
    }
    runTask();
  }

  private void runTask() {
    try {
      V result = task.call();
      setSuccess(result);
    } catch (Exception e) {
      setFailure(e);
    }
  }

  @Override
  public boolean isSuccess() {
    return result != null;
  }

  @Override
  public Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener) {
    checkNotNull(listener, "listener");

    synchronized (this) {
      listeners.add(listener);
    }
    if (isDone()) {
      notifyListeners();
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  private void notifyListeners() {
    if (!eventLoop.inEventLoop()) {
      return;
    }

    List<PromiseListener> listeners;

    synchronized (this) {
      if (this.listeners.isEmpty()) {
        return;
      }
      listeners = this.listeners;
      this.listeners = new ArrayList<>();
    }
    while (true) {
      for (PromiseListener listener : listeners) {
        try {
          listener.onComplete(this);
        } catch (Exception e) {
          LOGGER.error("A task terminated with unexpected exception. Exception: ", e);
        }
      }
      // At this point, listeners might be modified from other threads,
      // if more listeners added to list while executing this method, notify them also.
      // After notify them, initialize listeners to prevent double-notifying.
      synchronized (this) {
        if (this.listeners.isEmpty()) {
          return;
        }
        listeners = this.listeners;
        this.listeners = new ArrayList<>();
      }
    }
  }

  @Override
  public Promise<V> await(long timeout, TimeUnit unit) throws InterruptedException {
    checkPositiveOrZero(timeout, "timeout");

    long timeoutNanos = unit.toNanos(timeout);

    if (isDone()) {
      return this;
    }
    if (Thread.interrupted()) {
      throw new InterruptedException(toString());
    }

    long startTime = System.nanoTime();
    long timeLeft = timeoutNanos;
    while (true) {
      synchronized (this) {
        if (isDone()) {
          return this;
        }
        wait(timeLeft / 1000000);
        timeLeft = timeoutNanos - (System.nanoTime() - startTime);
        if (timeLeft <= 0) {
          return this;
        }
      }
    }
  }

  @Override
  public Promise<V> await() throws InterruptedException {
    if (isDone()) {
      return this;
    }
    if (Thread.interrupted()) {
      throw new InterruptedException(toString());
    }

    synchronized (this) {
      while (!isDone()) {
        wait();
      }
    }
    return this;
  }

  @Override
  public synchronized Promise<V> setSuccess(V result) {
    if (isDone()) {
      throw new IllegalStateException("Task already complete: " + this);
    }
    Object value = result == null ? EMPTY_RESULT : result;
    if (resultUpdater.compareAndSet(this, null, value)) {
      notifyAll();
      notifyListeners();
    }
    return this;
  }

  @Override
  public synchronized Promise<V> setFailure(Throwable cause) {
    if (isDone()) {
      throw new IllegalStateException("Task already complete: " + this);
    }
    if (causeUpdater.compareAndSet(this, null, cause)) {
      notifyAll();
      notifyListeners();
    }
    return this;
  }

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return result != null || cause != null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get() throws InterruptedException, ExecutionException {
    await();
    if (cause == null) {
      return (V) result;
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (!await(timeout, unit).isDone()) {
      throw new TimeoutException();
    }
    if (cause == null) {
      return (V) result;
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  protected EventLoop eventLoop() {
    return eventLoop;
  }
}
