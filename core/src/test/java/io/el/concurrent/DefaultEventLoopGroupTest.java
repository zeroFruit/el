package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class DefaultEventLoopGroupTest {

  private static final SingleThreadEventLoop eventLoop = new MockEventLoop(
      new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
  );

  private final Executor mockExecutor = mock(Executor.class);

  private final Runnable mockTask = mock(Runnable.class);

  private final EventLoopChooserFactory.EventLoopChooser chooser = () -> eventLoop;

  private final EventLoopChooserFactory eventLoopChooserFactory = loops -> chooser;

  private final DefaultEventLoopGroup defaultEventLoopGroup = new MockDefaultEventLoopGroup(
      10,
      mockExecutor,
      eventLoopChooserFactory
  );

  private final EventLoopChooserFactory mockEventLoopChooserFactory = mock(
      EventLoopChooserFactory.class
  );

  @BeforeEach
  public void setup() throws Exception {
    when(mockEventLoopChooserFactory.newChooser(any())).thenReturn(chooser);
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

  private static class MockDefaultEventLoopGroup extends DefaultEventLoopGroup {

    protected MockDefaultEventLoopGroup(int nThreads, Executor executor,
        EventLoopChooserFactory chooserFactory) {
      super(nThreads, executor, chooserFactory);
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
        TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      throw new UnsupportedOperationException();
    }

    @Override
    protected EventLoop newChild(Executor executor) {
      return eventLoop;
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
      return null;
    }
  }

  private static class MockFailedDefaultEventLoopGroup extends MockDefaultEventLoopGroup {

    protected MockFailedDefaultEventLoopGroup(int nThreads, Executor executor,
        EventLoopChooserFactory chooserFactory) {
      super(nThreads, executor, chooserFactory);
    }

    @Override
    protected EventLoop newChild(Executor executor) {
      try {
        throw new Exception();
      } catch (Exception e) {

      }
      return null;
    }
  }

  @Nested
  @DisplayName("On DefaultEventLoopGroup constructor")
  class DefaultEventLoopGroupConstructor {

    @Test
    @DisplayName("When construct with proper newChild() method, then shutdownGracefully and shutdown")
    void testConstructorSucceed() {
      try {
        new MockFailedDefaultEventLoopGroup(
            3,
            mockExecutor,
            eventLoopChooserFactory
        );
      } catch (IllegalStateException e) {
        assertFalse(defaultEventLoopGroup.isTerminated());
      }
    }

    @Test
    @DisplayName("When construct with failed newChild() method, then shutdownGracefully and shutdown")
    void testConstructorFailed() {
      try {
        new MockFailedDefaultEventLoopGroup(
            3,
            mockExecutor,
            eventLoopChooserFactory
        );
      } catch (IllegalStateException e) {
        assertTrue(defaultEventLoopGroup.isTerminated());
      }
    }
  }

  @Nested
  @DisplayName("On next() method")
  class NextMethod {

    @Test
    @DisplayName("When given mock chooser, then return random next event loop")
    void testNext() {
      EventLoop eventLoop = defaultEventLoopGroup.next();
      assertNotNull(eventLoop);
    }
  }

  @Nested
  @DisplayName("On shutdownGracefully() method")
  class ShutDownGracefullyMethod {

    @Test
    @DisplayName("When shutdownGracefully, then isShuttingDown true")
    void testShutdownGracefully() {
      boolean isShuttingDown = defaultEventLoopGroup.shutdownGracefully(15, TimeUnit.SECONDS);
      assertTrue(isShuttingDown);
      assertTrue(defaultEventLoopGroup.next().isShuttingDown());
    }
  }

  ;

  @Nested
  @DisplayName("On awaitTermination() method")
  class AwaitTerminationMethod {

    @Test
    @DisplayName("When mock task executed and awaitTermination, then return true")
    void testAwaitTerminationTrue() throws InterruptedException {
      boolean awaitTermination = false;
      try {
        defaultEventLoopGroup.execute(mockTask);
        awaitTermination = defaultEventLoopGroup.awaitTermination(1, TimeUnit.SECONDS);
      } finally {
        assertTrue(awaitTermination);
        assertTrue(defaultEventLoopGroup.next().isTerminated());
      }
    }
  }
}
