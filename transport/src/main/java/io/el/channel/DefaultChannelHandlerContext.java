package io.el.channel;

import io.el.concurrent.EventLoop;

public final class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

  private final ChannelHandler handler;

  DefaultChannelHandlerContext(
      String name, ChannelPipeline pipeline, EventLoop eventLoop, ChannelHandler handler) {
    super(name, pipeline, eventLoop, handler.getClass());
    this.handler = handler;
  }

  @Override
  public ChannelHandler handler() {
    return handler;
  }
}
