package io.el.connection;

public interface Channel extends ChannelOutboundInvoker {

  ChannelPipeline pipeline();

  boolean isRegistered();

  boolean isOpen();

  ChannelEventLoop channelEventLoop();

  void register(ChannelEventLoop eventLoop, ChannelPromise promise);
}
