package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static io.el.internal.ObjectUtil.checkPositiveOrZero;

import io.el.internal.DefaultPriorityQueue;
import io.el.internal.ObjectUtil;
import io.el.internal.PriorityQueue;
import io.el.internal.Time;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SingleThreadEventLoop extends AbstractExecutorService implements EventLoop {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final int INITIAL_QUEUE_CAPACITY = 16;
  private static final AtomicReferenceFieldUpdater<SingleThreadEventLoop, State> stateUpdater =
      AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventLoop.class, State.class, "state");
  private static final Comparator<ScheduledPromise<?>> SCHEDULED_FUTURE_TASK_COMPARATOR =
      ScheduledPromise::compareTo;
  private final Executor executor;
  private final Queue<Runnable> taskQueue;
  private final PriorityQueue<ScheduledPromise<?>> scheduledPromiseQueue;
  private volatile Thread thread;
  private volatile State state = State.NOT_STARTED;
  private long nextTaskId;
  private long shutdownStartNanos;
  private long shutdownTimeoutNanos;

  public SingleThreadEventLoop(Executor executor) {
    this.executor = executor;
    this.taskQueue = new LinkedBlockingDeque<>(INITIAL_QUEUE_CAPACITY);
    this.scheduledPromiseQueue = new DefaultPriorityQueue<>(
        INITIAL_QUEUE_CAPACITY,
        SCHEDULED_FUTURE_TASK_COMPARATOR);
  }

  @Override
  public boolean inEventLoop() {
    return Thread.currentThread() == this.thread;
  }

  @Override
  public boolean shutdownGracefully(long timeout, TimeUnit unit) {
    checkNotNull(unit, "unit");
    checkPositiveOrZero(timeout, "timeout");

    shutdownStartNanos = Time.currentNanos();

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

    shutdownTimeoutNanos = unit.toNanos(timeout);

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

  public ScheduledPromise<?> schedule(Runnable command, long delay, TimeUnit unit) {
    ObjectUtil.checkNotNull(command, "command");
    ObjectUtil.checkNotNull(unit, "unit");
    if (delay < 0L) {
      delay = 0L;
    }

    nextTaskId += 1;
    ScheduledPromise<?> task = new ScheduledPromise<>(
        this,
        command,
        ScheduledPromise.deadlineNanos(unit.toNanos(delay)));

    if (!inEventLoop()) {
      execute(task);
      return task;
    }

    scheduledPromiseQueue.add(task.setId(nextTaskId));
    return task;
  }

  public PriorityQueue<ScheduledPromise<?>> scheduledTaskQueue() {
    return scheduledPromiseQueue;
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
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
        LOGGER.error("An event loop terminated with unexpected exception. Exception:", t);
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
        } finally {
          // drain tasks
          int numTasks = drainTasks();
          if (numTasks > 0) {
            LOGGER.info("An event loop terminated with " +
                "non-empty task queue ({})", numTasks);
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
    cancelScheduledTasks();
    runAllTasks();
    if (Time.currentNanos() - shutdownStartNanos > shutdownTimeoutNanos) {
      return true;
    }
    return isShutdown();
  }

  private void cancelScheduledTasks() {
    scheduledTaskQueue().clear();
  }

  protected Runnable takeTask() {
    if (!inEventLoop()) {
      return null;
    }
    BlockingQueue<Runnable> taskQueue = (BlockingQueue<Runnable>) this.taskQueue;
    while (true) {
      ScheduledPromise<?> scheduledPromise = peekScheduledTask();
      if (scheduledPromise == null) {
        return taskQueue.poll();
      }
      queueScheduledTask();
      return taskQueue.poll();
    }
  }

  private void addTask(Runnable task) {
    checkNotNull(task, "task");
    if (isShuttingDown()) {
      throw new RejectedExecutionException("Event loop is terminating");
    }
    if (task instanceof ScheduledPromise) {
      scheduledPromiseQueue.add((ScheduledPromise<?>) task);
      return;
    }
    if (!taskQueue.offer(task)) {
      throw new RejectedExecutionException("Event loop failed to add task");
    }
  }

  private void runAllTasks() {
    if (!inEventLoop()) {
      return;
    }
    while (true) {
      Runnable task = takeTask();
      if (task == null) {
        break;
      }
      try {
        task.run();
      } catch (Throwable t) {
        LOGGER.error("An event loop terminated with unexpected exception. Exception: ", t);
      }
    }
  }

  private int drainTasks() {
    int numTasks = 0;
    if (!inEventLoop()) {
      return 0;
    }
    while (true) {
      Runnable task = takeTask();
      if (task == null) {
        break;
      }
      numTasks += 1;
    }
    return numTasks;
  }

  private ScheduledPromise<?> peekScheduledTask() {
    return this.scheduledPromiseQueue.peek();
  }

  private Runnable pollScheduledTask(long nanoTime) {
    if (!inEventLoop()) {
      return null;
    }
    ScheduledPromise<?> scheduledPromise = peekScheduledTask();
    if (scheduledPromise == null || scheduledPromise.deadlineNanos() - nanoTime > 0) {
      return null;
    }
    scheduledPromiseQueue.remove();
    return scheduledPromise;
  }

  private boolean queueScheduledTask() {
    if (scheduledPromiseQueue.isEmpty()) {
      return true;
    }
    while (true) {
      Runnable scheduledTask = pollScheduledTask(Time.currentNanos());
      if (scheduledTask == null) {
        return true;
      }
      boolean isAdded = taskQueue.offer(scheduledTask);
      if (isAdded) {
        continue;
      }
      scheduledPromiseQueue.add((ScheduledPromise<?>) scheduledTask);
      return false;
    }
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
