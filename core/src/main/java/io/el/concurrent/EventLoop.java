package io.el.concurrent;

import java.util.concurrent.TimeUnit;

public interface EventLoop {
    boolean inEventLoop();
    <V> Promise<V> newPromise();
    boolean shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);
}
