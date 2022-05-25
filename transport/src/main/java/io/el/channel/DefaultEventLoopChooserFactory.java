package io.el.channel;

import io.el.concurrent.EventLoop;
import io.el.concurrent.EventLoopChooserFactory;
import java.util.List;

public class DefaultEventLoopChooserFactory implements EventLoopChooserFactory {

  @Override
  public EventLoopChooser newChooser(List<EventLoop> loops) {
    return null;
  }
}
