package io.el.channel;

import io.el.concurrent.EventLoop;

public interface ChannelHandlerContext extends ChannelInboundInvoker, ChannelOutboundInvoker {
  /** Return the {@link Channel} which is bound to the {@link ChannelHandlerContext} */
  Channel channel();

  /** Returns the {@link EventLoop} which is used to execute an asynchronous task */
  EventLoop eventLoop();

  /** Returns the {@link ChannelPipeline} that is bound this {@link ChannelHandlerContext} */
  ChannelPipeline pipeline();

  /** Returns the {@link ChannelHandler} that is bound this {@link ChannelHandlerContext} */
  ChannelHandler handler();

  /**
   * The unique name of the {@link ChannelHandlerContext}. The name is used when then {@link
   * ChannelHandler} was added to the {@link ChannelPipeline}. This name can also be used to access
   * the registered {@link ChannelHandler} from the {@link ChannelPipeline}
   */
  String name();

  @Override
  ChannelHandlerContext fireChannelRegistered();

  @Override
  ChannelHandlerContext fireExceptionCaught(Throwable t);
}
