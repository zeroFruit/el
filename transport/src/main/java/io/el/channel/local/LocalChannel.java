package io.el.channel.local;

import io.el.channel.AbstractChannel;
import io.el.channel.Channel;
import io.el.channel.ChannelId;
import io.el.channel.ChannelPromise;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;

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

    @Override
    public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
      if (!isOpen()) {
        promise.setFailure(new ClosedChannelException());
        return;
      }

      if (state == State.CONNECTED) {
        AlreadyConnectedException cause = new AlreadyConnectedException();
        promise.setFailure(cause);
        pipeline().fireExceptionCaught(cause);
        return;
      }

      if (state != State.BOUND) {
        if (localAddress == null) {
          localAddress = new LocalAddress(LocalChannel.this);
        }
      }

      if (localAddress != null) {
        try {
          doBind(localAddress);
        } catch (Throwable t) {
          promise.setFailure(t);
          // TODO: call close
        }
      }

      Channel boundChannel = LocalChannelRegistry.get(remoteAddress);
      if (!(boundChannel instanceof LocalServerChannel)) {
        Exception cause = new ConnectException("Connection refused: " + remoteAddress);
        promise.setFailure(cause);
        // TODO: call close
      }

      LocalServerChannel serverChannel = (LocalServerChannel) boundChannel;
      // TODO: implement serve
      //      peer = serverChannel.serve(LocalChannel.this);
    }

    @Override
    public void doBind(SocketAddress localAddress) {
      // TODO: implement me
    }
  }
}
