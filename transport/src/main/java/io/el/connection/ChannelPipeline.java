package io.el.connection;

public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker {

  ChannelPipeline addLast(ChannelHandler... handlers);

  ChannelPipeline remove(ChannelHandler handler);

  Channel channel();

  ChannelHandlerContext firstContext();

  ChannelHandlerContext context(ChannelHandler handler);

  @Override
  ChannelPipeline fireChannelRegistered();

  @Override
  ChannelPipeline fireChannelRead(Object msg);
}
