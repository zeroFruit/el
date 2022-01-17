package io.el.channel;

public interface ChannelHandler {
  void handlerAdded(ChannelHandlerContext ctx) throws Exception;
}
