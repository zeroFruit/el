package io.el.channel;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ChannelInitializer<C extends Channel> implements ChannelInboundHandler {

  private final Set<ChannelHandlerContext> initMap =
      Collections.newSetFromMap(new ConcurrentHashMap<ChannelHandlerContext, Boolean>());

  /**
   * This method will be called only once when the {@link Channel} is registered. After the method
   * returns this instance will be removed from the {@link ChannelPipeline}
   */
  protected abstract void initChannel(C channel) throws Exception;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().isRegistered()) {
      if (initChannel(ctx)) {
        removeState(ctx);
      }
    }
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    // Normally this method will never be called as handlerAdded() should call initChannel()
    // and remove the handler
    if (initChannel(ctx)) {
      ctx.pipeline().fireChannelRegistered();
      removeState(ctx);
    } else {
      ctx.fireChannelRegistered();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {}

  private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
    if (!initMap.add(ctx)) {
      return false;
    }
    try {
      initChannel((C) ctx.channel());
    } catch (Throwable cause) {
      exceptionCaught(ctx, cause);
    } finally {
      ChannelPipeline pipeline = ctx.pipeline();
      if (pipeline.context(this) != null) {
        pipeline.remove(this);
      }
      return true;
    }
  }

  private void removeState(final ChannelHandlerContext ctx) {
    ctx.eventLoop().execute(() -> initMap.remove(ctx));
  }
}
