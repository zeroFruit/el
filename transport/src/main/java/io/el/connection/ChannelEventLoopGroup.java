package io.el.connection;

import io.el.concurrent.EventLoop;
import io.el.concurrent.Promise;

public interface ChannelEventLoopGroup {

  EventLoop next();

  ChannelPromise register(Channel channel);

  Promise<?> shutdownGracefully();
}
