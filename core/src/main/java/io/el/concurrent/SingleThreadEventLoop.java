package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static io.el.internal.ObjectUtil.checkPositiveOrZero;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class SingleThreadEventLoop extends AbstractEventLoop {

    private static final AtomicReferenceFieldUpdater<SingleThreadEventLoop, State> stateUpdater =
            AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventLoop.class, State.class, "state");

    private volatile Thread thread;

    private volatile State state = State.NOT_STARTED;

    private final Queue<Runnable> taskQueue;
    private final Executor executor;

    public SingleThreadEventLoop (Executor executor) {
        this.executor = executor;
        this.taskQueue = new LinkedBlockingDeque<>(16);
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == this.thread;
    }
    @Override
    public <V> Task<V> newTask() {
        return new DefaultTask<V>(this);
    }

    @Override
    public boolean shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        if (!inEventLoop()) {
            return false;
        }
        checkNotNull(unit, "unit");
        checkPositiveOrZero(quietPeriod, "quietPeriod");
        if (timeout < quietPeriod) {
            throw new IllegalArgumentException(
                    "timeout: " + timeout + " (expected >= quietPeriod (" + quietPeriod + "))");
        }

        if (isShuttingDown()) {
            return true;
        }

        while (true) {
            if (isShuttingDown()) {
                return true;
            }
            if (stateUpdater.compareAndSet(this, state, State.SHUTTING_DOWN)) {
                break;
            }
        }
        return true;
    }

    private boolean isShuttingDown() {
        return state.compareTo(State.SHUTTING_DOWN) >= 0;
    }

    @Override
    public boolean isShutdown() {
        return state.compareTo(State.SHUTDOWN) >= 0;
    }

    @Override
    public boolean isTerminated() {
        return state.equals(State.STARTED);
    }

    @Override
    public void execute(Runnable task) {
        checkNotNull(task, "task");
        addTask(task);
        if (inEventLoop()) {
            return;
        }
        start();
    }

    protected abstract void run();

    private void start() {
        if (!state.equals(State.NOT_STARTED)) {
            return;
        }
        if (!stateUpdater.compareAndSet(this, State.NOT_STARTED, State.STARTED)) {
            return;
        }
        boolean success = false;
        try {
            doStart();
            success = true;
        } finally {
            if (!success) {
                stateUpdater.compareAndSet(this, State.STARTED, State.NOT_STARTED);
            }
        }
    }

    private void doStart() {
        if (thread != null) {
            return;
        }
        executor.execute(() -> {
            thread = Thread.currentThread();
            try {
                SingleThreadEventLoop.this.run();
            } catch (Throwable t) {
                // TODO: Error handling
            } finally {
                while (true) {
                    if (state.compareTo(State.SHUTTING_DOWN) >= 0 ||
                            stateUpdater.compareAndSet(SingleThreadEventLoop.this, state, State.SHUTTING_DOWN)) {
                        break;
                    }
                }
                try {
                    // Run all remaining tasks
                    while (true) {
                        if (confirmShutdown()) {
                            break;
                        }
                    }
                    // Now we want to make sure no more tasks can be added from this point.
                    while (true) {
                        if (state.compareTo(State.SHUTDOWN) >= 0 ||
                                stateUpdater.compareAndSet(SingleThreadEventLoop.this, state, State.SHUTDOWN)) {
                            break;
                        }
                    }
                    // We have the final set of tasks in the queue now, no more can be added, run all remaining.
                    // No need to loop here, this is the final pass.
                    confirmShutdown();
                } finally {
                    // drain tasks
                    int numUserTasks = drainTasks();
                    if (numUserTasks > 0) {
                        System.out.println("An event executor terminated with " +
                                "non-empty task queue (" + numUserTasks + ')');
                    }
                }
            }
        });
    }

    protected boolean confirmShutdown() {
        if (!isShuttingDown()) {
            return false;
        }
        if (!inEventLoop()) {
            throw new IllegalStateException("must be invoked from an event loop");
        }
        runAllTasks();
        return isShutdown();
    }

    protected Runnable takeTask() {
        if (!inEventLoop()) {
            return null;
        }
        BlockingQueue<Runnable> taskQueue = (BlockingQueue<Runnable>) this.taskQueue;
        while (true) {
            Runnable task = null;
            try {
                task = taskQueue.take();
            } catch (InterruptedException e) { }
            return task;
        }
    }

    private void addTask(Runnable task) {
        checkNotNull(task, "task");
        if (isShutdown()) {
            throw new RejectedExecutionException("EventLoop terminated");
        }
        taskQueue.add(task);
    }

    private void runAllTasks() {
        if (!inEventLoop()) {
            return;
        }
        while (true) {
            Runnable task = taskQueue.poll();
            if (task == null) {
                break;
            }
            execute(task);
        }
    }

    private int drainTasks() {
        int numTasks = 0;
        if (!inEventLoop()) {
            return 0;
        }
        while (true) {
            Runnable task = taskQueue.poll();
            if (task == null) {
                break;
            }
            numTasks += 1;
        }
        return numTasks;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    enum State {
        NOT_STARTED(1),
        STARTED(2),
        SHUTTING_DOWN(3),
        SHUTDOWN(4),
        TERMINATED(5);

        int value;

        State(int value) {
            this.value = value;
        }
    }
}
