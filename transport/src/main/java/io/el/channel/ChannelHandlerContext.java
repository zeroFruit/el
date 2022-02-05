package io.el.channel;

import io.el.concurrent.EventLoop;

// event loop를 관리
public interface ChannelHandlerContext extends ChannelInboundInvoker, ChannelOutboundInvoker {
  Channel channel();
  EventLoop eventLoop();
  ChannelHandlerContext fireChannelRegistered();
  ChannelPipeline pipeline();
  ChannelHandler handler();
}
