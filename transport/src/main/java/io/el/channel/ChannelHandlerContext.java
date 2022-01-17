package io.el.channel;

import io.el.concurrent.EventLoop;

public interface ChannelHandlerContext extends ChannelInboundInvoker, ChannelOutboundInvoker {
  Channel channel();
  EventLoop eventLoop();
  ChannelHandlerContext fireChannelRegistered();
  ChannelPipeline pipeline();
  ChannelHandler handler();
}
