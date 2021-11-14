package io.el.connection;

import java.net.SocketAddress;

public interface ChannelOutboundHandler extends ChannelHandler {
  void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;

  void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
      SocketAddress localAddress, ChannelPromise promise) throws Exception;

  void read(ChannelHandlerContext ctx) throws Exception;

  void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;
}
