package io.el.channel;

public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker {
  /** Add handlers to its handler chain at the end. */
  ChannelPipeline addLast(ChannelHandler... handlers);

  /** Remove handler */
  ChannelPipeline remove(ChannelHandler handler);

  /** Returns channel it binds to. */
  Channel channel();

  /** Returns first handler context */
  ChannelHandlerContext firstContext();

  /** Returns the context of the handler */
  ChannelHandlerContext context(ChannelHandler handler);

  /**
   * A {@link Channel} was registered to its {@link ChannelEventLoop}.
   *
   * <p>This will result in having the {@link
   * ChannelInboundHandler#channelRegistered(ChannelHandlerContext)} method called of the next
   * {@link ChannelInboundHandler} contained in the {@link ChannelPipeline} of the {@link Channel}.
   */
  @Override
  ChannelPipeline fireChannelRegistered();
}
