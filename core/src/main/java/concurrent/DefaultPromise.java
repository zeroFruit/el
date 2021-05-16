package concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static internal.ObjectUtil.checkNotNull;
import static internal.ObjectUtil.checkPositiveOrZero;

public class DefaultPromise<V> implements Promise<V> {
    private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> resultUpdater =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");
    private static final AtomicReferenceFieldUpdater<DefaultPromise, Throwable> causeUpdater =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Throwable.class, "cause");

    private volatile Object result;
    private volatile Throwable cause;

    private final EventLoop eventLoop;

    private List<PromiseListener> listeners = new ArrayList<>();

    public DefaultPromise(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    @Override
    public boolean isSuccess() {
        return result != null;
    }

    @Override
    public Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            listeners.add(listener);
        }
        if (isDone()) {
            notifyListeners();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void notifyListeners() {
        if (!eventLoop.inEventLoop()) {
            return;
        }

        List<PromiseListener> listeners;

        synchronized (this) {
            if (this.listeners.isEmpty()) {
                return;
            }
            listeners = this.listeners;
            this.listeners = new ArrayList<>();
        }
        while (true) {
            for (PromiseListener listener : listeners) {
                try {
                    listener.onComplete(this);
                } catch (Exception e) {
                    // FIXME: logging error
                    e.printStackTrace();
                }
            }
            // At this point, listeners might be modified from other threads,
            // if more listeners added to list while executing this method, notify them also.
            // After notify them, initialize listeners to prevent double-notifying.
            synchronized (this) {
                if (this.listeners.isEmpty()) {
                    return;
                }
                listeners = this.listeners;
                this.listeners = new ArrayList<>();
            }
        }
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        checkPositiveOrZero(timeout, "timeout");

        long timeoutNanos = unit.toNanos(timeout);

        if (isDone()) {
            return true;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long timeLeft = timeoutNanos;
        while (true) {
            synchronized (this) {
                if (isDone()) {
                    return true;
                }
                wait(timeLeft / 1000000);
                if (isDone()) {
                    return true;
                }
                timeLeft = timeoutNanos - (System.nanoTime() - startTime);
                if (timeLeft <= 0) {
                    return isDone();
                }
            }
        }
    }

    @Override
    public Promise<V> setSuccess(V result) {
        if (isDone()) {
            throw new IllegalStateException("Promise already complete: " + this);
        }
        if (resultUpdater.compareAndSet(this, null, result)) {
            notifyListeners();
        }
        return this;
    }

    @Override
    public Promise<V> setFailure(Throwable cause) {
        if (isDone()) {
            throw new IllegalStateException("Promise already complete: " + this);
        }
        if (causeUpdater.compareAndSet(this, null, cause)) {
            notifyListeners();
        }
        return this;
    }

    // TODO
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    // TODO
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return result != null || cause != null;
    }

    // TODO
    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    // TODO
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
