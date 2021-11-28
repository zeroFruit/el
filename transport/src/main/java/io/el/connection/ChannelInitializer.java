package io.el.connection;

public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

  protected abstract void initChannel(C ch) throws Exception;

  @Override
  public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    if (initChannel(ctx)) {
      ctx.pipeline().fireChannelRegistered();
    } else {
      ctx.fireChannelRegistered();
    }
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().isRegistered()) {
      initChannel(ctx);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
    try {
      initChannel((C) ctx.channel());
    } catch (Throwable cause) {
      // TODO: error-handling
    } finally {
      ChannelPipeline pipeline = ctx.pipeline();
      if (pipeline.context(this) != null) {
        pipeline.remove(this);
      }
    }
    return true;
  }
}
