package io.el.connection.nio;

import io.el.connection.AbstractChannel;

public class AbstractNioChannel extends AbstractChannel {

  public void finishConnect() {

  }

  public void forceFlush() {

  }

  public void read() {

  }

  @Override
  public boolean isOpen() {
    return false;
  }
}
