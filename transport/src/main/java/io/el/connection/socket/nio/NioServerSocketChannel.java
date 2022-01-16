package io.el.connection.socket.nio;

import io.el.connection.ChannelOutboundBuffer;
import io.el.connection.nio.AbstractNioMessageChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

public class NioServerSocketChannel extends AbstractNioMessageChannel implements
    io.el.connection.socket.ServerSocketChannel {

  private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

  private static ServerSocketChannel newSocket(SelectorProvider provider) {
    try {
      return provider.openServerSocketChannel();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to open a server socket.", e);
    }
  }

  public NioServerSocketChannel() {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER));
  }

  public NioServerSocketChannel(ServerSocketChannel channel) {
    super(channel, SelectionKey.OP_ACCEPT);
  }

  protected NioServerSocketChannel(SelectableChannel ch, int readInterestOp) {
    super(ch, readInterestOp);
  }

  @Override
  protected int doReadMessages(List<Object> buf) throws Exception {
    // TODO: implement me
    return 0;
  }

  @Override
  protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ServerSocketChannel javaChannel() {
    return (ServerSocketChannel) super.javaChannel();
  }

  @Override
  protected void doBind(SocketAddress localAddress) throws Exception {
    javaChannel().bind(localAddress);
  }

  @Override
  public boolean isActive() {
    return isOpen() && javaChannel().socket().isBound();
  }

  @Override
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void doFinishConnect() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public InetSocketAddress localAddress() {
    return (InetSocketAddress) super.localAddress();
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) super.remoteAddress();
  }

  @Override
  protected SocketAddress getLocalAddress() {
    return javaChannel().socket().getLocalSocketAddress();
  }

  @Override
  protected SocketAddress getRemoteAddress() {
    return null;
  }
}
