package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class DefaultEventLoopGroupTest {

  private final Executor mockExecutor = mock(Executor.class);
  private final Runnable mockTask = mock(Runnable.class);
  private final SingleThreadEventLoop eventLoop = new MockEventLoop(
      new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
  );

  private final EventLoopChooserFactory.EventLoopChooser chooser = () -> eventLoop;

  private final EventLoopChooserFactory eventLoopChooserFactory = loops -> chooser;
  private final DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(10,
      mockExecutor, eventLoopChooserFactory) {
    @Override
    protected EventLoop newChild(Executor executor) {
      return eventLoop;
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
      return null;
    }
  };
  private final EventLoopChooserFactory mockEventLoopChooserFactory = mock(
      EventLoopChooserFactory.class);

  @BeforeEach
  public void setup() throws Exception {
    when(mockEventLoopChooserFactory.newChooser(any())).thenReturn(chooser);
  }

  @Test
  @DisplayName("When given mock chooser, then return random next event loop")
  void testNext() {
    EventLoop eventLoop = defaultEventLoopGroup.next();
    assertNotNull(eventLoop);
  }

  @Test
  @DisplayName("When call iterator, then return iterator")
  void testIterator() {
    Iterator<EventLoop> eventLoopIterator = defaultEventLoopGroup.iterator();
    assertNotNull(eventLoopIterator);
  }

  @Test
  @DisplayName("When shutdownGracefully, then isShuttingDown true")
  void testShutdownGracefully() {
    boolean isShuttingDown = defaultEventLoopGroup.shutdownGracefully(15, TimeUnit.SECONDS);
    assertTrue(isShuttingDown);
    assertTrue(defaultEventLoopGroup.next().isShuttingDown());
  }

  @Test
  @DisplayName("When mock task executed and awaitTermination, then return true")
  void testAwaitTermination() throws InterruptedException {
    boolean awaitTermination = false;
    try {
      defaultEventLoopGroup.execute(mockTask);
      awaitTermination = defaultEventLoopGroup.awaitTermination(1, TimeUnit.SECONDS);
    } finally {
      assertTrue(awaitTermination);
      assertTrue(defaultEventLoopGroup.next().isTerminated());
    }
  }

  private static class MockEventLoop extends SingleThreadEventLoop {

    private static final AtomicReferenceFieldUpdater<MockEventLoop, State> stateUpdater =
        AtomicReferenceFieldUpdater.newUpdater(MockEventLoop.class, State.class, "state");
    private final CountDownLatch threadLock = new CountDownLatch(1);
    private volatile State state = State.NOT_STARTED;

    public MockEventLoop(Executor executor) {
      super(executor);
    }

    @Override
    protected void run() {
      while (!confirmShutdown()) {
        Runnable task = takeTask();
        if (task != null) {
          task.run();
        }
      }
    }

    @Override
    public void execute(Runnable task) {
      checkNotNull(task, "task");
      if (inEventLoop()) {
        return;
      }
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
          stateUpdater.compareAndSet(this, State.STARTED, State.TERMINATED);
        }
      }
    }

    private void doStart() {
      try {

      } finally {
        stateUpdater.compareAndSet(this, State.STARTED, State.TERMINATED);
      }
    }


    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      if (inEventLoop()) {
        throw new IllegalStateException("cannot await termination of the current thread");
      }

      try {
        threadLock.await(timeout, unit);
      } catch (InterruptedException e) {
      }
      return isTerminated();
    }

    @Override
    public boolean isTerminated() {
      return state.equals(State.TERMINATED);
    }
  }
}
