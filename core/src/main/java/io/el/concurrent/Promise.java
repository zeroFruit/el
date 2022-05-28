package io.el.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** The result of an asynchronous operation. */
public interface Promise<V> extends Future<V>, Runnable {

  /** Returns {@code true} if and only if this asynchronous task done with success. */
  boolean isSuccess();

  /**
   * Marks this asynchronous task as success and notifies all listeners.
   *
   * <p>If it is already success or failed then throws IllegalStateException
   */
  Promise<V> setSuccess(V result);

  /**
   * Add the listener to this promise. Listener will get notified if this asynchronous task done
   * with success or failure.
   */
  Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener);

  /** Waits this asynchronous task to be done with specified time limit */
  Promise<V> await(long timeout, TimeUnit unit) throws InterruptedException;

  /** Waits this asynchronous task to be done. */
  Promise<V> await() throws InterruptedException;

  /**
   * Marks this asynchronous task as failure and notifies all listeners.
   *
   * <p>If it is already success or failed then throws IllegalStateException
   */
  Promise<V> setFailure(Throwable cause);
}
