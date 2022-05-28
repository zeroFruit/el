package io.el.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** A {@code EventLoop} represents the runner for running asynchronous computation */
public interface EventLoop extends ExecutorService {

  /**
   * Returns {@code true} if current thread is running for {@link EventLoop}.
   *
   * @return {@code true} if current thread is running for {@link EventLoop}.
   */
  boolean inEventLoop();

  /**
   * Attempts to terminate thread. Before terminating, {@link EventLoop} waits for all the task in
   * the loop to be completed. After {@code timeout}, all the tasks in the loop drained and not
   * executed.
   *
   * @return {@code true} if the {@link EventLoop} terminates without exception.
   */
  boolean shutdownGracefully(long timeout, TimeUnit unit);

  /**
   * Check if event loop state is ShuttindDown.
   *
   * @return {@code true} if {@link EventLoop} state is {@code State.SHUTTING_DOWN}
   */
  boolean isShuttingDown();

  @Override
  <V> Promise<V> submit(Callable<V> task);

  @Override
  Promise<?> submit(Runnable task);

  ScheduledPromise<?> schedule(Runnable command, long delay, TimeUnit unit);

  <V> ScheduledPromise<V> schedule(Callable<V> command, long delay, TimeUnit unit);
}
