package io.el.connection;

public interface ChannelInboundHandler extends ChannelHandler {

  void channelRegistered(ChannelHandlerContext ctx) throws Exception;

  void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;
}
