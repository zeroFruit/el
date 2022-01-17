package io.el.channel;

public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker {
  ChannelPipeline addLast(ChannelHandler... handlers);
  ChannelPipeline remove(ChannelHandler handler);
  Channel channel();
  ChannelHandlerContext firstContext();
  ChannelHandlerContext context(ChannelHandler handler);
  @Override
  ChannelPipeline fireChannelRegistered();
}
