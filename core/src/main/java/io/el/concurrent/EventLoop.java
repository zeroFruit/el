package io.el.concurrent;

import java.util.concurrent.TimeUnit;

public interface EventLoop {

  boolean inEventLoop();

  <V> Promise<V> newTask();

  boolean shutdownGracefully(long timeout, TimeUnit unit);
}
