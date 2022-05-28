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
      return loops.get(new RoundRobin(loops.size()).next());
    }
  }

  private static class RoundRobin {

    private final AtomicInteger idx = new AtomicInteger(-1);
    private final int total;

    public RoundRobin(int total) {
      this.total = total;
    }

    public int next() {
      return this.idx.incrementAndGet() % this.total;
    }
  }
}
