package io.el.concurrent;

import java.util.List;

public interface EventLoopChooserFactory {
  EventLoopChooser newChooser(List<EventLoop> loops);

  interface EventLoopChooser {
    EventLoop next();
  }
}
