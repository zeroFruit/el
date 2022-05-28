package io.el.channel;

import java.util.concurrent.ThreadFactory;

public class DefaultThreadFactory implements ThreadFactory {

  // TODO: change this more specifically
  @Override
  public Thread newThread(Runnable r) {
    return new Thread(r);
  }
}
