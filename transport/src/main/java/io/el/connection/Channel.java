package io.el.connection;

import java.net.SocketAddress;

public interface Channel extends ChannelOutboundInvoker {

  ChannelPipeline pipeline();

  boolean isOpen();

  boolean isRegistered();

  boolean isActive();

  ChannelEventLoop channelEventLoop();

  SocketAddress localAddress();

  SocketAddress remoteAddress();

  Internal internal();

  interface Internal {

    SocketAddress localAddress();

    SocketAddress remoteAddress();

    void register(ChannelEventLoop eventLoop, ChannelPromise promise);

    void bind(SocketAddress localAddress, ChannelPromise promise);

    void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);
  }
}
