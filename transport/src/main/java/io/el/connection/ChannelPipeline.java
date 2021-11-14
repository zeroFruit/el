package io.el.connection;

public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker {

  ChannelPipeline addLast(ChannelHandler... handlers);

  @Override
  ChannelPipeline fireChannelRegistered();

  @Override
  ChannelPipeline fireChannelRead(Object msg);

  Channel channel();
}
