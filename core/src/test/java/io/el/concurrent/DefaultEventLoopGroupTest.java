package io.el.concurrent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class DefaultEventLoopGroupTest {

  private final Executor mockExecutor = mock(Executor.class);

  private final SingleThreadEventLoop eventLoop = new SingleThreadEventLoop(
      new ThreadPerTaskExecutor(Executors.defaultThreadFactory())
  ) {

    @Override
    protected void run() {
      while (!confirmShutdown()) {
        Runnable task = takeTask();
        if (task != null) {
          task.run();
        }
      }
    }
  };

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

  // disabled because termination function is not working yet.
  @Test
  @Disabled
  @DisplayName("When awaitTermination, then return true")
  void testAwaitTermination() throws InterruptedException {
    boolean awaitTermination = false;
    try {
      awaitTermination = defaultEventLoopGroup.awaitTermination(1, TimeUnit.SECONDS);
    } finally {
      assertTrue(awaitTermination);
//      assertTrue(defaultEventLoopGroup.next().isTerminated());
    }
  }
}
