package io.el.concurrent;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {
  /**
   * Returns one of its EventLoop it manages. Usually it depends on the {@link io.el.concurrent.EventLoopChooserFactory.EventLoopChooser}
   * */
  EventLoop next();

  @Override
  Iterator<EventLoop> iterator();

  /**
  * Submit task to one of its EventLoop. {@link EventLoopGroup} just delegate task it receive.
  * */
  @Override
  <V> Promise<V> submit(Callable<V> task);

  /**
   * Submit task to one of its EventLoop. {@link EventLoopGroup} just delegate task it receive.
   * */
  @Override
  Promise<?> submit(Runnable task);
}
