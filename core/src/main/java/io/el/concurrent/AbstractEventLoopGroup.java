package io.el.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractEventLoopGroup implements EventLoopGroup {
    @Override
    public <V> Promise<V> submit(Callable<V> task) {
        return next().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    @Override
    public Promise<?> submit(Runnable task) {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public ScheduledPromise<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public <V> ScheduledPromise<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public void execute(Runnable command) {
        next().execute(command);
    }
}
