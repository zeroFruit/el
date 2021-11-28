package io.el.connection;

import java.net.SocketAddress;

/**
 * A skeletal server-side {@link Channel} implementation.  A server-side
 * {@link Channel} does not allow the following operations:
 */
public abstract class AbstractServerChannel extends AbstractChannel implements Channel {

  @Override
  public AbstractInternal newInternal() {
    return new DefaultServerInternal();
  }

  private final class DefaultServerInternal extends AbstractInternal {

    @Override
    public SocketAddress localAddress() {
      return AbstractServerChannel.super.localAddress();
    }

    @Override
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      promise.setFailure(new UnsupportedOperationException());
    }
  }
}
