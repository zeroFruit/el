package io.el.concurrent;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface EventLoopGroup extends ExecutorService, Iterable<EventLoop> {
    EventLoop next();

    @Override
    Iterator<EventLoop> iterator();

    @Override
    <V> Promise<V> submit(Callable<V> task);

    @Override
    Promise<?> submit(Runnable task);

    ScheduledPromise<?> schedule(Runnable command, long delay, TimeUnit unit);

    <V> ScheduledPromise<V> schedule(Callable<V> command, long delay, TimeUnit unit);

    boolean shutdownGracefully(long timeout, TimeUnit unit);
}
