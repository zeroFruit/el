package io.el.concurrent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class MultiThreadEventLoop extends AbstractEventLoopGroup {

    private final List<EventLoop> children;

    private final EventLoopChooserFactory.EventLoopChooser chooser;

    protected MultiThreadEventLoop(
            int nThreads, Executor executor,
            EventLoopChooserFactory chooserFactory, Object... args) {
        children = Arrays.asList(new EventLoop[nThreads]);

        for (int i = 0; i < nThreads; i += 1) {
            boolean success = false;
            try {
                children.set(i, newChild(executor, args));
                success = true;
            } catch (Exception e) {
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    // TODO: error handling
                }
            }
        }

        chooser = chooserFactory.newChooser(children);
    }

    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

    @Override
    public EventLoop next() {
        return chooser.next();
    }


    @Override
    public Iterator<EventLoop> iterator() {
        return children.iterator();
    }

    @Override
    public void shutdown() {
        // TODO:
    }

    @Override
    public boolean isShutdown() {
        // TODO:
        return false;
    }

    @Override
    public boolean isTerminated() {
        // TODO:
        return false;
    }


    @Override
    public List<Runnable> shutdownNow() {
        // TODO:
        return null;
    }


    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO:
        return false;
    }
}
