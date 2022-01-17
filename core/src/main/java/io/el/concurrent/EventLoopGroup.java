package io.el.concurrent;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {
  EventLoop next();

  @Override
  Iterator<EventLoop> iterator();

  @Override
  <V> Promise<V> submit(Callable<V> task);

  @Override
  Promise<?> submit(Runnable task);
}
