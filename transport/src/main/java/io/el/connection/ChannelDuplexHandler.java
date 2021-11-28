package io.el.connection;

import java.net.SocketAddress;

public class ChannelDuplexHandler extends ChannelInboundHandlerAdapter
    implements ChannelOutboundHandler {

  @Override
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
      throws Exception {
    ctx.bind(localAddress, promise);
  }

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
      SocketAddress localAddress, ChannelPromise promise) throws Exception {
    ctx.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public void read(ChannelHandlerContext ctx) throws Exception {
    ctx.read();
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    ctx.write(msg, promise);
  }
}
