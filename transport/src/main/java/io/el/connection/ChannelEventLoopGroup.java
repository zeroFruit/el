package io.el.connection;

public interface ChannelEventLoopGroup {

  ChannelEventLoop next();

  ChannelPromise register(Channel channel);
}
