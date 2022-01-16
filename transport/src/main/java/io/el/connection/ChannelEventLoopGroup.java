package io.el.connection;

import io.el.concurrent.EventLoopGroup;

public interface ChannelEventLoopGroup extends EventLoopGroup {

  ChannelEventLoop next();

  ChannelPromise register(Channel channel);
}
