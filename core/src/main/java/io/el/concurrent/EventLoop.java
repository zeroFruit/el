package io.el.concurrent;

import java.util.concurrent.TimeUnit;

public interface EventLoop {

  boolean inEventLoop();

  <V> Task<V> newTask();

  boolean shutdownGracefully(long timeout, TimeUnit unit);
}
