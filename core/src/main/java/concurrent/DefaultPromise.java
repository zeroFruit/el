package concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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
    public Promise<V> await(long timeout, TimeUnit unit) throws InterruptedException {
        checkPositiveOrZero(timeout, "timeout");

        long timeoutNanos = unit.toNanos(timeout);

        if (isDone()) {
            return this;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long timeLeft = timeoutNanos;
        while (true) {
            synchronized (this) {
                if (isDone()) {
                    return this;
                }
                wait(timeLeft / 1000000);
                timeLeft = timeoutNanos - (System.nanoTime() - startTime);
                if (timeLeft <= 0) {
                    return this;
                }
            }
        }
    }

    @Override
    public Promise<V> await() throws InterruptedException {
        if (isDone()) {
            return this;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        synchronized (this) {
            while (!isDone()) {
                wait();
            }
        }
        return this;
    }

    @Override
    public synchronized Promise<V> setSuccess(V result) {
        if (isDone()) {
            throw new IllegalStateException("Promise already complete: " + this);
        }
        if (resultUpdater.compareAndSet(this, null, result)) {
            notifyAll();
            notifyListeners();
        }
        return this;
    }

    @Override
    public synchronized Promise<V> setFailure(Throwable cause) {
        if (isDone()) {
            throw new IllegalStateException("Promise already complete: " + this);
        }
        if (causeUpdater.compareAndSet(this, null, cause)) {
            notifyAll();
            notifyListeners();
        }
        return this;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (isCancelled()) {
            throw new IllegalStateException("Promise already cancelled: " + this);
        }
        if (causeUpdater.compareAndSet(this, null, new CancellationException())) {
            notifyAll();
            notifyListeners();
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cause instanceof CancellationException;
    }

    @Override
    public boolean isDone() {
        return result != null || cause != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        await();
        if (cause == null) {
            return (V) result;
        }
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!await(timeout, unit).isDone()) {
            throw new TimeoutException();
        }
        if (cause == null) {
            return (V) result;
        }
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }
}
