package io.el.channel;

import java.net.SocketAddress;

/**
 * A {@link AbstractServerChannel} accepts an incoming connection attempt and creates a {@link
 * Channel}s by accepting them. A server-side {@link Channel} does not allow the following
 * operations:
 *
 * <ul>
 *   <li>{@link #connect(SocketAddress)}
 * </ul>
 */
public abstract class AbstractServerChannel extends AbstractChannel {

  protected AbstractServerChannel() {
    super();
  }

  protected AbstractServerChannel(ChannelId id) {
    super(id);
  }

  @Override
  public SocketAddress remoteAddress() {
    return internal().remoteAddress();
  }

  private abstract class AbstractServerInternal extends AbstractInternal {
    @Override
    public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
      promise.setFailure(new UnsupportedOperationException());
    }
  }
}
