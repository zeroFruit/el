package io.el.channel.local;

import io.el.channel.AbstractChannel;
import io.el.channel.Channel;
import io.el.channel.ChannelId;
import io.el.channel.ChannelPromise;
import io.el.internal.ObjectUtil;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;

public class LocalChannel extends AbstractChannel {

  public enum State {
    BOUND,
    CONNECTED,
    CLOSED
  }

  private volatile State state;

  private volatile LocalServerChannel server;
  private volatile LocalAddress localAddress;
  private volatile LocalAddress remoteAddress;
  private volatile ChannelPromise connected;

  public LocalChannel() {}

  public LocalChannel(ChannelId id) {
    super(id);
  }

  @Override
  protected LocalInternal newInternal() {
    return new LocalInternal();
  }

  @Override
  protected void doRegister() {
    // NO-OP
  }

  protected void doConnect(LocalServerChannel server, ChannelPromise result) {
    this.server = server;
    server.connectFrom(this);
    // TODO: implement serve
    //      peer = serverChannel.serve(LocalChannel.this);
    state = State.CONNECTED;
    remoteAddress = this.server.localAddress();
    result.setSuccess(null);
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

      if (connected != null) {
        throw new IllegalStateException("connection is in pending");
      }
      connected = promise;

      if (state != State.BOUND && localAddress == null) {
        localAddress = new LocalAddress(LocalChannel.this);
      }

      if (localAddress != null) {
        try {
          doBind(localAddress);
        } catch (Throwable t) {
          promise.setFailure(t);
          // TODO: call close
          return;
        }
      }

      Channel boundChannel = LocalChannelRegistry.get(remoteAddress);
      if (!(boundChannel instanceof LocalServerChannel)) {
        Exception cause = new ConnectException("Connection refused: " + remoteAddress);
        promise.setFailure(cause);
        // TODO: call close
        return;
      }

      doConnect((LocalServerChannel) boundChannel, promise);
    }

    @Override
    public void doBind(SocketAddress localAddress) {
      ObjectUtil.checkNotNull(localAddress(), "localAddress");
      LocalChannel.this.localAddress =
          LocalChannelRegistry.register(LocalChannel.this, localAddress);
      state = State.BOUND;
    }
  }
}
