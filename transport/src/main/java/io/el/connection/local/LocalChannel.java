package io.el.connection.local;

import io.el.connection.AbstractChannel;
import io.el.connection.Channel;
import io.el.connection.ChannelOutboundBuffer;
import io.el.connection.ChannelOutboundInvoker;
import io.el.connection.ChannelPipeline;
import io.el.connection.ChannelPromise;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.util.LinkedList;
import java.util.Queue;

public class LocalChannel extends AbstractChannel {

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelOutboundInvoker write(Object msg, ChannelPromise promise) {
    return null;
  }

  private enum State { OPEN, BOUND, CONNECTED, CLOSED }

  // in netty this is SPSC queue.
  final Queue<Object> inboundBuffer = new LinkedList<>();

  private final Runnable readTask = new Runnable() {
    @Override
    public void run() {
      if (!inboundBuffer.isEmpty()) {
        readInbound();
      }
    }
  };

  private volatile State state;
  private volatile LocalChannel peer;
  private volatile LocalAddress localAddress;
  private volatile LocalAddress remoteAddress;
  private volatile ChannelPromise connectPromise;
  private volatile boolean readInProgress;
  private volatile boolean writeInProgress;

  public LocalChannel() {
    super();
  }

  protected LocalChannel(LocalChannel peer) {
    this.peer = peer;
  }

  @Override
  public boolean isOpen() {
    return state != State.CLOSED;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public SocketAddress localAddress() {
    return null;
  }

  @Override
  public SocketAddress remoteAddress() {
    return null;
  }

  @Override
  protected void doRegister() {
    if (peer == null) {
      return;
    }
    final LocalChannel peer = this.peer;
    state = State.CONNECTED;

    peer.remoteAddress = null;
    peer.state = State.CONNECTED;

    // Always call peer.eventLoop().execute() even if peer.eventLoop().inEventLoop() is true.
    // This ensures that if both channels are on the same event loop, the peer's channelActive
    // event is triggered *after* this channel's channelRegistered event, so that this channel's
    // pipeline is fully initialized by ChannelInitializer before any channelRead events.
    peer.channelEventLoop().execute(new Runnable() {
      @Override
      public void run() {
        ChannelPromise promise = peer.connectPromise;

        // Only trigger fireChannelActive() if the promise was not null and was not completed yet.
        // connectPromise may be set to null if doClose() was called in the meantime.
        if (promise == null) {
          return;
        }
        promise.setSuccess(null);
        // peer.pipeline().fireChannelActive();
      }
    });
  }

  @Override
  protected void doBind(SocketAddress localAddress) {
    this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
    state = State.BOUND;
  }

  @Override
  protected void doBeginRead() throws Exception {
    if (readInProgress) {
      return;
    }
    Queue<Object> inboundBuffer = this.inboundBuffer;
    if (inboundBuffer.isEmpty()) {
      readInProgress = true;
      return;
    }
    try {
      channelEventLoop().execute(readTask);
    } catch (Throwable cause) {
      // TODO: error-handling
    }

  }

  @Override
  protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    switch (state) {
    case OPEN:
    case BOUND:
      throw new NotYetConnectedException();
    case CLOSED:
      throw new ClosedChannelException();
    case CONNECTED:
      break;
    }

    final LocalChannel peer = this.peer;
    writeInProgress = true;
    try {
      ClosedChannelException ex = null;
      for (;;) {
        Object msg = in.current();
        if (msg == null) {
          break;
        }
        try {
          // It is possible the peer could have closed while we are writing, and in this case we should
          // simulate real socket behavior and ensure the write operation is failed.
          if (peer.state != State.CONNECTED) {
            ex = new ClosedChannelException();
            continue;
          }
          peer.inboundBuffer.add(msg);
        } catch (Throwable cause) {
          // TODO: error handling
        }
      }
    } finally {
      // The following situation may cause trouble:
      // 1. Write (with promise X)
      // 2. promise X is completed when in.remove() is called, and a listener on this promise calls close()
      // 3. Then the close event will be executed for the peer before the write events, when the write events
      // actually happened before the close event.
      writeInProgress = false;
    }
  }

  @Override
  protected AbstractInternal newInternal() {
    return new LocalInternal();
  }

  private void readInbound() {
    ChannelPipeline pipeline = pipeline();
    do {
      Object received = inboundBuffer.poll();
      if (received == null) {
        break;
      }
      pipeline.fireChannelRead(received);
    } while (true);
    // pipeline.fireChannelReadComplete();
  }

  private class LocalInternal extends AbstractInternal {

    @Override
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      if (!ensureOpen(promise)) {
        return;
      }
      if (state == State.CONNECTED) {
        promise.setFailure(new AlreadyConnectedException());
        return;
      }
      if (connectPromise != null) {
        throw new ConnectionPendingException();
      }
      connectPromise = promise;
      if (state != State.BOUND && localAddress == null) {
        localAddress = new LocalAddress(LocalChannel.this);
      }
      if (localAddress != null) {
        try {
          doBind(localAddress);
        } catch (Throwable t) {
          promise.setFailure(t);
          return;
        }
      }
      Channel boundChannel = LocalChannelRegistry.get(remoteAddress);
      if (!(boundChannel instanceof LocalServerChannel)) {
        promise.setFailure(new ConnectException("connection refused: " + remoteAddress));
        return;
      }
      LocalServerChannel serverChannel = (LocalServerChannel) boundChannel;
      peer = serverChannel.serve(LocalChannel.this);
    }
  }
}
