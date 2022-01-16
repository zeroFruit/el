package io.el.connection.local;

import io.el.connection.AbstractServerChannel;
import io.el.connection.ChannelOutboundBuffer;
import io.el.connection.ChannelOutboundInvoker;
import io.el.connection.ChannelPipeline;
import io.el.connection.ChannelPromise;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;

public class LocalServerChannel extends AbstractServerChannel {

  private volatile int state; // 0 - open, 1 - active, 2 - closed
  private volatile LocalAddress localAddress;
  private volatile boolean acceptInProgress;
  private final Queue<Object> inboundBuffer = new ArrayDeque<>();

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isActive() {
    return state == 1;
  }

  @Override
  protected LocalAddress getLocalAddress() {
    return (LocalAddress) super.localAddress();
  }

  @Override
  protected LocalAddress getRemoteAddress() {
    return (LocalAddress) super.remoteAddress();
  }

  @Override
  protected void doBind(SocketAddress localAddress) {

  }

  @Override
  protected void doBeginRead() throws Exception {

  }

  @Override
  protected void doWrite(ChannelOutboundBuffer in) throws Exception {

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

  LocalChannel serve(final LocalChannel peer) {
    final LocalChannel child = newLocalChannel(peer);
    if (channelEventLoop().inEventLoop()) {
      doServe(child);
      return child;
    }
    channelEventLoop().execute(new Runnable() {
      @Override
      public void run() {
        doServe(child);
      }
    });
    return child;
  }

  /**
   * A factory method for {@link LocalChannel}s. Users may override it
   * to create custom instances of {@link LocalChannel}s.
   */
  protected LocalChannel newLocalChannel(LocalChannel peer) {
    return new LocalChannel(peer);
  }

  private void doServe(final LocalChannel child) {
    inboundBuffer.add(child);
    if (acceptInProgress) {
      acceptInProgress = false;
      readInbound();
    }
  }

  private void readInbound() {
    ChannelPipeline pipeline = pipeline();
    while (true) {
      Object m = inboundBuffer.poll();
      if (m == null) {
        break;
      }
      pipeline.fireChannelRead(m);
    }
  }
}
