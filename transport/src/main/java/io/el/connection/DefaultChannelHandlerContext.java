package io.el.connection;

import io.el.concurrent.EventLoop;

public class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

  private final ChannelHandler handler;

  DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, EventLoop eventLoop,
      String name, ChannelHandler handler) {
    super(pipeline, eventLoop, name, handler.getClass());
    this.handler = handler;
  }

  @Override
  public ChannelHandler handler() {
    return handler;
  }
}
