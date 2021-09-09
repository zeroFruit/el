package io.el.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * A {@code EventLoop} represents the runner for running asynchronous computation
 * */
public interface EventLoop {

  /**
   * Returns {@code true} if current thread is running for {@link EventLoop}.
   *
   * @return {@code true} if current thread is running for {@link EventLoop}.
   * */
  boolean inEventLoop();

  /**
   * Attempts to terminate thread. Before terminating, {@link EventLoop} waits for
   * all the task in the loop to be completed. After {@code timeout}, all the tasks
   * in the loop drained and not executed.
   *
   * @return {@code true} if the {@link EventLoop} terminates without exception.
   * */
  boolean shutdownGracefully(long timeout, TimeUnit unit);
}
