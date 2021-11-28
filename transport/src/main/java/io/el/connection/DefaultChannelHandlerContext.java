package io.el.connection;

import io.el.concurrent.EventLoop;
import java.net.SocketAddress;

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

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelHandlerContext read() {
    return null;
  }

  @Override
  public ChannelOutboundInvoker write(Object msg, ChannelPromise promise) {
    return null;
  }
}
