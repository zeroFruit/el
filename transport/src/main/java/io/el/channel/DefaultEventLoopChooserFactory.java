package io.el.channel;

import io.el.concurrent.EventLoop;
import io.el.concurrent.EventLoopChooserFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultEventLoopChooserFactory implements EventLoopChooserFactory {

  @Override
  public EventLoopChooser newChooser(List<EventLoop> loops) {
    return new DefaultEventLooptChooser(loops);
  }

  private static final class DefaultEventLooptChooser implements EventLoopChooser {

    private final List<EventLoop> loops;

    private DefaultEventLooptChooser(List<EventLoop> loops) {
      this.loops = loops;
    }

    @Override
    public EventLoop next() {
      AtomicIntegerRoundRobin atomicIntegerRoundRobin = new AtomicIntegerRoundRobin(loops.size());
      atomicIntegerRoundRobin.index();
      return loops.get(atomicIntegerRoundRobin.index());
    }
  }

  public static class AtomicIntegerRoundRobin {

    private final AtomicInteger atomicInteger = new AtomicInteger(-1);
    private final int totalIndexes;

    public AtomicIntegerRoundRobin(int totalIndexes) {
      this.totalIndexes = totalIndexes;
    }

    public int index() {
      int currentIndex;
      int nextIndex;

      do {
        currentIndex = this.atomicInteger.get();
        nextIndex = currentIndex < Integer.MAX_VALUE ? currentIndex + 1 : 0;
      } while (!this.atomicInteger.compareAndSet(currentIndex, nextIndex));

      return nextIndex % this.totalIndexes;
    }
  }
}
