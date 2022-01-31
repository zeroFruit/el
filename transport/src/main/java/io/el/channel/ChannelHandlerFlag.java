package io.el.channel;

final class ChannelHandlerFlag {
  static final int FLAG_INBOUND = 1;
  static final int FLAG_OUTBOUND = 1;

  static int flag(Class<? extends ChannelHandler> clazz) {
    if (ChannelInboundHandler.class.isAssignableFrom(clazz)) {
      return FLAG_INBOUND;
    }
    return FLAG_OUTBOUND;
  }
}
