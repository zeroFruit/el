package io.el.channel;

import java.net.SocketAddress;

public interface ChannelOutboundHandler extends ChannelHandler {
  /**
   * Called once a bind operation is made.
   *
   * @param ctx the {@link ChannelHandlerContext} for which the bind operation is made
   * @param localAddress the {@link SocketAddress} to which it should bound
   * @param promise the {@link ChannelPromise} to notify once the operation completes
   */
  void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
      throws Exception;

  /**
   * Called once a connect operation is made.
   *
   * @param ctx the {@link ChannelHandlerContext} for which the connect operation is made
   * @param remoteAddress the {@link SocketAddress} to which it should connect
   * @param localAddress the {@link SocketAddress} which is used as source on connect
   * @param promise the {@link ChannelPromise} to notify once the operation completes
   */
  void connect(
      ChannelHandlerContext ctx,
      SocketAddress remoteAddress,
      SocketAddress localAddress,
      ChannelPromise promise)
      throws Exception;
}
