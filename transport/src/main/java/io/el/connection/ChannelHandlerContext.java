package io.el.connection;

import io.el.concurrent.EventLoop;

public interface ChannelHandlerContext {

  Channel channel();

  EventLoop eventLoop();

  ChannelHandlerContext fireChannelRegistered();

  ChannelHandlerContext fireChannelRead(Object msg);

  ChannelPipeline pipeline();

  ChannelHandler handler();
}
