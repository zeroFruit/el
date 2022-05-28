package io.el.channel.local;

import io.el.channel.AbstractChannel;
import io.el.channel.ChannelId;

public class LocalChannel extends AbstractChannel {

  private enum State {
    OPEN,
    BOUND,
    CONNECTED,
    CLOSED
  }

  private volatile State state;
  private volatile LocalServerChannel server;
  private volatile LocalChannel peer;
  private volatile LocalAddress localAddress;
  private volatile LocalAddress remoteAddress;

  public LocalChannel(ChannelId id) {
    super(id);
  }

  protected LocalChannel(ChannelId id, LocalServerChannel server, LocalChannel peer) {
    super(id);
    this.peer = peer;
    this.server = server;
    this.localAddress = server.localAddress();
    this.remoteAddress = (LocalAddress) peer.localAddress();
  }

  @Override
  protected AbstractInternal newInternal() {
    return new LocalInternal();
  }

  @Override
  protected void doRegister() {
    if (peer == null || server == null) {
      return;
    }
    final LocalChannel peer = this.peer;
    state = State.CONNECTED;
    peer.remoteAddress = (LocalAddress) localAddress();
    peer.state = State.CONNECTED;
  }

  @Override
  public boolean isOpen() {
    return state != State.CLOSED;
  }

  @Override
  public boolean isActive() {
    return state == State.CONNECTED;
  }

  private class LocalInternal extends AbstractInternal {

    @Override
    public LocalAddress localAddress() {
      return localAddress;
    }

    @Override
    public LocalAddress remoteAddress() {
      return remoteAddress;
    }
  }
}
