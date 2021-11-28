package io.el.connection.nio;

import io.el.concurrent.Promise;
import io.el.connection.ChannelEventLoopTaskQueueFactory;
import io.el.connection.ChannelSingleThreadEventLoop;
import io.el.connection.SelectStrategy;
import io.el.connection.util.IntSupplier;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class NioChannelEventLoop extends ChannelSingleThreadEventLoop {

  // nextWakeupNanos is:
  //    AWAKE            when EL is awake
  //    NONE             when EL is waiting with no wakeup scheduled
  //    other value T    when EL is waiting with wakeup scheduled at time T
  private static final long AWAKE = -1L;
  private static final long NONE = Long.MAX_VALUE;
  private final SelectStrategy selectStrategy;
  private final AtomicLong nextWakeupNanos = new AtomicLong(AWAKE);
  private volatile int ioRatio = 50;

  private Selector selector;
  private final SelectorProvider selectorProvider;
  private final IntSupplier selectNowSupplier = new IntSupplier() {
    @Override
    public int get() throws Exception {
      return selectNow();
    }
  };
  private boolean needsToSelectAgain;

  public NioChannelEventLoop(NioChannelEventLoopGroup parent, Executor executor,
      SelectorProvider selectorProvider, SelectStrategy strategy,
      ChannelEventLoopTaskQueueFactory tailTaskQueueFactory) {
    super(parent, executor, tailTaskQueueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS));
    this.selectStrategy = strategy;
    this.selectorProvider = selectorProvider;
    this.selector = openSelector(this.selectorProvider);
  }

  private Selector openSelector(SelectorProvider provider) {
    final Selector unwrappedSelector;
    try {
      unwrappedSelector = provider.openSelector();
    } catch (IOException e) {
      // TODO: error-handling
      throw new IllegalStateException("failed to open a new selector", e);
    }
    return unwrappedSelector;
  }

  /**
   * Sets the percentage of the desired amount of time spent for I/O in the event loop. Value range
   * from 1-100. The default value is {@code 50}, which means the event loop will try to spend the
   * same amount of time for I/O as for non-I/O tasks. The lower the number the more time can be
   * spent on non-I/O tasks. If value set to {@code 100}, this feature will be disabled and event
   * loop will not attempt to balance I/O and non-I/O tasks.
   */
  public void setIoRatio(int ioRatio) {
    if (ioRatio <= 0 || ioRatio > 100) {
      throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
    }
    this.ioRatio = ioRatio;
  }

  @Override
  protected void run() {
    int selectCnt = 0;
    while (true) {
      try {
        int strategy;
        try {
          strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
          switch (strategy) {
            case SelectStrategy.CONTINUE:
              continue;
            case SelectStrategy.BUSY_WAIT:
              // fall-through to SELECT since the busy-wait is not supported with NIO
            case SelectStrategy.SELECT:
              long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
              if (curDeadlineNanos == -1) {
                curDeadlineNanos = NONE;
              }
              nextWakeupNanos.set(curDeadlineNanos);
              try {
                if (!hasTasks()) {
                  strategy = select(curDeadlineNanos);
                }
              } finally {
                nextWakeupNanos.lazySet(AWAKE);
              }
            default:
          }
        } catch (Exception e) {
          selectCnt = 0;
          continue;
        }

        selectCnt += 1;
        needsToSelectAgain = false;
        boolean ranTasks;
        if (strategy > 0) {
          final long ioStartTime = System.nanoTime();
          try {
            processSelectedKeys();
          } finally {
            final long ioTime = System.nanoTime() - ioStartTime;
            ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
          }
        } else {
          ranTasks = runAllTasks(0); // This will run the minimum number of tasks
        }
      } catch (Exception e) {
        // TODO: error-handling
      } finally {
        // TODO: termination
      }
    }
  }

  int selectNow() throws IOException {
    return selector.selectNow();
  }

  private void processSelectedKeys() {
    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    if (selectedKeys.isEmpty()) {
      return;
    }
    Iterator<SelectionKey> i = selectedKeys.iterator();
    while (true) {
      final SelectionKey k = i.next();
      final Object a = k.attachment();
      i.remove();

      if (a instanceof AbstractNioChannel) {
        processSelectedKey(k, (AbstractNioChannel) a);
      }
      if (!i.hasNext()) {
        break;
      }
      if (!needsToSelectAgain) {
        continue;
      }
      selectAgain();
      selectedKeys = selector.selectedKeys();

      if (selectedKeys.isEmpty()) {
        break;
      }
      // Create the iterator again to avoid ConcurrentModificationException
      i = selectedKeys.iterator();
    }
  }

  private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    final AbstractNioChannel.NioInternal internal = ch.internal();
    if (!k.isValid()) {
      // TODO: error handling
      return;
    }
    try {
      int readyOps = k.readyOps();
      if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
        // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
        // See https://github.com/netty/netty/issues/924
        int ops = k.interestOps();
        ops &= ~SelectionKey.OP_CONNECT;
        k.interestOps(ops);

        internal.finishConnect();
      }

      // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
      if ((readyOps & SelectionKey.OP_WRITE) != 0) {
        // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
        // internal.forceFlush();
      }

      // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
      // to a spin loop
      if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
        ch.read();
      }
    } catch (CancelledKeyException ignored) {
      // TODO: error handling
    }
  }

  private int select(long deadlineNanos) throws IOException {
    if (deadlineNanos == NONE) {
      return selector.select();
    }
    // FIXME:
    return selector.selectNow();
  }

  private void selectAgain() {
    needsToSelectAgain = false;
    try {
      selector.selectNow();
    } catch (Throwable t) {
      // TODO: error handling
    }
  }
}
