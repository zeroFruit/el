package io.el.channel;

/**
 * {@link ChannelHandlerFlag} helps to determine whether {@link ChannelHandler} is inbound or
 * outbound
 */
final class ChannelHandlerFlag {
  static final int FLAG_INBOUND = 0;
  static final int FLAG_OUTBOUND = 1;
  static final int FLAG_INOUTBOUND = 2;

  static int flag(Class<? extends ChannelHandler> clazz) {
    if (ChannelInboundHandler.class.isAssignableFrom(clazz)
        && ChannelOutboundHandler.class.isAssignableFrom(clazz)) {
      return FLAG_INOUTBOUND;
    }
    if (ChannelInboundHandler.class.isAssignableFrom(clazz)) {
      return FLAG_INBOUND;
    }
    return FLAG_OUTBOUND;
  }

  static boolean isNotInbound(int executionFlag) {
    return executionFlag != FLAG_INBOUND && executionFlag != FLAG_INOUTBOUND;
  }

  static boolean isNotOutbound(int executionFlag) {
    return executionFlag != FLAG_OUTBOUND && executionFlag != FLAG_INOUTBOUND;
  }
}
