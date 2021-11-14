package io.el.connection;

public class ChannelInboundHandlerAdapter extends ChannelHandlerAdapter implements
    ChannelInboundHandler {

  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelRegistered();
  }

  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ctx.fireChannelRead(msg);
  }
}
