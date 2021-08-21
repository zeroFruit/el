package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static io.el.internal.ObjectUtil.checkPositiveOrZero;

import io.el.internal.DefaultPriorityQueue;
import io.el.internal.ObjectUtil;
import io.el.internal.PriorityQueue;
import io.el.internal.Time;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class SingleThreadEventLoop extends AbstractEventLoop {
  private static final int INITIAL_QUEUE_CAPACITY = 16;
  private static final AtomicReferenceFieldUpdater<SingleThreadEventLoop, State> stateUpdater =
      AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventLoop.class, State.class, "state");
  private final Executor executor;
  private volatile Thread thread;
  private volatile State state = State.NOT_STARTED;

  private Queue<Runnable> taskQueue;
  private PriorityQueue<ScheduledTask<?>> scheduledTaskQueue;

  private long nextTaskId;

  public SingleThreadEventLoop(Executor executor) {
    this.executor = executor;
    this.taskQueue = new LinkedBlockingDeque<>(INITIAL_QUEUE_CAPACITY);
    this.scheduledTaskQueue = new DefaultPriorityQueue<>(INITIAL_QUEUE_CAPACITY);
  }

  @Override
  public boolean inEventLoop() {
    return Thread.currentThread() == this.thread;
  }

  @Override
  public <V> Task<V> newTask() {
    return new DefaultTask<V>(this);
  }

  // TODO: Add test case
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

  public ScheduledTask<?> schedule(Runnable command, long delay, TimeUnit unit) {
    ObjectUtil.checkNotNull(command, "command");
    ObjectUtil.checkNotNull(unit, "unit");
    if (delay < 0L) {
      delay = 0L;
    }

    nextTaskId += 1;
    ScheduledTask<?> task = new ScheduledTask<>(
        this,
        command,
        ScheduledTask.deadlineNanos(unit.toNanos(delay)));

    if (!inEventLoop()) {
      execute(task);
      return task;
    }

    scheduledTaskQueue.add(task.setId(nextTaskId));
    return task;
  }

  public PriorityQueue<ScheduledTask<?>> scheduledTaskQueue() {
    return scheduledTaskQueue;
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
      ScheduledTask<?> scheduledTask = peekScheduledTask();
      if (scheduledTask == null) {
        return taskQueue.poll();
      }
      queueScheduledTask();
      return taskQueue.poll();
    }
  }

  private void addTask(Runnable task) {
    checkNotNull(task, "task");
    if (isShutdown()) {
      throw new RejectedExecutionException("EventLoop terminated");
    }
    if (task instanceof ScheduledTask) {
      scheduledTaskQueue.add((ScheduledTask<?>) task);
      return;
    }
    taskQueue.add(task);
  }

  // FIXME: Tasks in the scheduledTaskQueue should be moved to taskQueue, then process them all
  private void runAllTasks() {
    if (!inEventLoop()) {
      return;
    }
    while (true) {
      Runnable task = scheduledTaskQueue.poll();
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
      Runnable task = scheduledTaskQueue.poll();
      if (task == null) {
        break;
      }
      numTasks += 1;
    }
    return numTasks;
  }

  private ScheduledTask<?> peekScheduledTask() {
    return this.scheduledTaskQueue.peek();
  }

  private Runnable pollScheduledTask(long nanoTime) {
    if (!inEventLoop()) {
      return null;
    }
    ScheduledTask<?> scheduledTask = peekScheduledTask();
    if (scheduledTask == null || scheduledTask.deadlineNanos() - nanoTime > 0) {
      return null;
    }
    scheduledTaskQueue.remove();
    return scheduledTask;
  }

  private boolean queueScheduledTask() {
    if (scheduledTaskQueue.isEmpty()) {
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
      scheduledTaskQueue.add((ScheduledTask<?>) scheduledTask);
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
