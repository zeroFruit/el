package io.el.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;

public abstract class AbstractEventLoop extends AbstractExecutorService implements EventLoop {

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }
}
