package io.el.channel;

public interface ChannelInboundInvoker {

  /**
   * A {@link Channel} was registered to its {@link ChannelEventLoop}.
   *
   * This will result in having the {@link ChannelInboundHandler#channelRegistered(ChannelHandlerContext)}
   * method called of the next {@link ChannelInboundHandler} contained in the {@link ChannelPipeline}
   * of the {@link Channel}
   * */
  ChannelInboundInvoker fireChannelRegistered();

  /**
   * A {@link Channel} received an one of its inbound operation
   *
   * This will result in having the {@link ChannelInboundHandler#exceptionCaught(ChannelHandlerContext, Throwable)}
   * method called of the next {@link ChannelInboundHandler} contained in the {@link ChannelPipeline}
   * of the {@link Channel}
   * */
  ChannelInboundInvoker fireExceptionCaught(Throwable cause);
}
