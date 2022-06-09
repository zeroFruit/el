package io.el.channel.local;

import io.el.channel.AbstractServerChannel;
import io.el.channel.ChannelId;
import io.el.channel.local.LocalChannel.State;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

public class LocalServerChannel extends AbstractServerChannel {

  // localAddress specifies host address. Address is set at binding step
  private volatile LocalAddress localAddress;
  private volatile LocalChannel client;
  private volatile State state;

  public LocalServerChannel() {}

  protected LocalServerChannel(ChannelId id) {
    super(id);
  }

  @Override
  protected LocalServerInternal newInternal() {
    return new LocalServerInternal();
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
    return state != State.CLOSED;
  }

  @Override
  public boolean isActive() {
    return state == State.CONNECTED;
  }

  /**
   * connectFrom is called when client tries to connect to the {@link LocalServerChannel}. This
   * assumes that the client is making the connection with the server.
   */
  protected void connectFrom(LocalChannel client) {
    this.client = client;
    this.state = State.CONNECTED;
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

    @Override
    public void doBind(SocketAddress localAddress) {
      ObjectUtil.checkNotNull(localAddress, "localAddress");
      LocalServerChannel.this.localAddress =
          LocalChannelRegistry.register(LocalServerChannel.this, localAddress);
      state = State.BOUND;
    }
  }
}
