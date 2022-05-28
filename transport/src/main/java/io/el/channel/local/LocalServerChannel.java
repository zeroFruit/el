package io.el.channel.local;

import io.el.channel.AbstractServerChannel;
import io.el.channel.ChannelId;
import java.net.SocketAddress;

public class LocalServerChannel extends AbstractServerChannel {

  // localAddress specifies host address. Address is set at binding step
  private volatile LocalAddress localAddress;

  protected LocalServerChannel(ChannelId id) {
    super(id);
  }

  @Override
  protected AbstractInternal newInternal() {
    return null;
  }

  @Override
  public LocalAddress localAddress() {
    return localAddress;
  }

  @Override
  public LocalAddress remoteAddress() {
    return (LocalAddress) super.remoteAddress();
  }

  @Override
  protected void doRegister() {
    // NO-OP
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  protected LocalChannel newLocalChannel(ChannelId id, LocalChannel peer) {
    return new LocalChannel(id, this, peer);
  }

  private class LocalServerInternal extends AbstractInternal {

    @Override
    public SocketAddress localAddress() {
      return localAddress;
    }

    @Override
    public SocketAddress remoteAddress() {
      // local server channel has no remote address
      return null;
    }
  }
}
