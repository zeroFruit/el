package io.el.channel;

/**
 * {@link ChannelHandler} which adds callbacks for state changes. This allows the user to hook in to
 * state changes easily.
 */
public interface ChannelInboundHandler extends ChannelHandler {
  /**
   * Called when {@link Channel} of the {@link ChannelHandlerContext} was registered with its {@link
   * ChannelEventLoop}
   */
  void channelRegistered(ChannelHandlerContext ctx) throws Exception;

  /** Gets called if a {@link Throwable} was thrown. */
  void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
