package io.el.connection.nio;

import io.el.concurrent.Promise;
import io.el.connection.AbstractChannel;
import io.el.connection.ChannelPromise;
import io.el.connection.ChannelPromiseListener;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;

public abstract class AbstractNioChannel extends AbstractChannel {

  private final SelectableChannel ch;
  protected final int readInterestOp;
  volatile SelectionKey selectionKey;
  boolean readPending;

  /**
   * The future of the current connection attempt.  If not null, subsequent
   * connection attempts will fail.
   */
  private ChannelPromise connectPromise;
  private Promise<?> connectTimouetPromise;
  private SocketAddress requestedRemoteAddress;

  protected AbstractNioChannel(SelectableChannel ch, int readInterestOp) {
    this.ch = ch;
    this.readInterestOp = readInterestOp;
    try {
      ch.configureBlocking(false);
    } catch (IOException e) {
      // TODO: error-handling
      try {
        ch.close();
      } catch (IOException e2) {
        // TODO: error-handling
      }

    }
  }

  @Override
  public boolean isOpen() {
    return ch.isOpen();
  }

  @Override
  public NioInternal internal() {
    return (NioInternal) super.internal();
  }

  @Override
  protected void doBeginRead() throws Exception {
    // Channel.read() or ChannelHandlerContext.read() was called
    final SelectionKey selectionKey = this.selectionKey;
    if (!selectionKey.isValid()) {
      return;
    }
    readPending = true;
    final int interestOps = selectionKey.interestOps();
    if ((interestOps & readInterestOp) == 0) {
      selectionKey.interestOps(interestOps | readInterestOp);
    }
  }

  protected SelectableChannel javaChannel() {
    return ch;
  }

  protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;

  protected abstract void doFinishConnect() throws Exception;

  public interface NioInternal extends Internal {
    SelectableChannel ch();

    void finishConnect();

    void read();
  }

  protected abstract class AbstractNioInternal extends AbstractInternal
      implements NioInternal {
    @Override
    public final SelectableChannel ch() {
      return ch;
    }

    @Override
    public final void connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      if (!ensureOpen(promise)) {
        return;
      }
      if (connectPromise != null) {
        throw new ConnectionPendingException();
      }

      try {
        boolean wasActive = isActive();
        if (doConnect(remoteAddress, localAddress)) {
          fulfillConnectPromise(promise, wasActive);
          return;
        }
        connectPromise = promise;
        requestedRemoteAddress = remoteAddress;

        // Schedule connect timeout
        int connectTimeoutMillis = 10000; // FIXME
        connectTimouetPromise = channelEventLoop().schedule(new Runnable() {
          @Override
          public void run() {
            ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
            if (connectPromise != null && !connectPromise.isDone()) {
              connectPromise.setFailure(new IllegalStateException(
                  "connection timed out: " + remoteAddress));
              // TODO: close
            }
          }
        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);

        promise.addListener(new ChannelPromiseListener() {
          @Override
          public void onComplete(ChannelPromise promise) throws Exception {

          }
        });
      } catch (Throwable t) {
        // TODO: error-handling
      }
    }

    private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
      if (promise == null) {
        // Closed via cancellation and the promise has been notified already.
        return;
      }

      promise.setSuccess(null);
    }

    @Override
    public void finishConnect() {
      // Note this method is invoked by the event loop only if the connection attempt was
      // neither cancelled nor timed out.
      assert channelEventLoop().inEventLoop();
      try {
        doFinishConnect();
      } catch (Throwable t) {
        // TODO: error-handling
      } finally {
        connectPromise = null;
      }
    }
  }

}
