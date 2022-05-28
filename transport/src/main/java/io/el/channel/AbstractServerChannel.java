package io.el.channel;

import java.net.SocketAddress;

public abstract class AbstractServerChannel extends AbstractChannel {

  protected AbstractServerChannel(ChannelId id) {
    super(id);
  }

  @Override
  public SocketAddress remoteAddress() {
    return null;
  }

  @Override
  protected AbstractInternal newInternal() {
    return null;
  }

  private abstract class AbstractServerInternal extends AbstractInternal {
    @Override
    public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
      promise.setFailure(new UnsupportedOperationException());
    }
  }
}
