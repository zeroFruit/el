package io.el.concurrent;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {
  /**
   * Returns one of its EventLoop it manages. Usually it depends on the {@link
   * io.el.concurrent.EventLoopChooserFactory.EventLoopChooser}
   */
  EventLoop next();

  @Override
  Iterator<EventLoop> iterator();

  /**
   * Attempts to terminate all children, which are event loop lists.
   *
   * @return {@code true} if none of {@link EventLoop}s terminates without exception.
   */
  boolean shutdownGracefully(long timeout, TimeUnit unit);

  /**
   * Check if event loops are shutting down.
   *
   * @return {@code true} if every child's state is ShuttingDown, return true.
   */
  boolean isShuttingDown();

  /** Submit task to one of its EventLoop. {@link EventLoopGroup} just delegate task it receive. */
  @Override
  <V> Promise<V> submit(Callable<V> task);

  /** Submit task to one of its EventLoop. {@link EventLoopGroup} just delegate task it receive. */
  @Override
  Promise<?> submit(Runnable task);
}
