package io.el.channel;

import io.el.concurrent.EventLoopGroup;

public interface ChannelEventLoopGroup extends EventLoopGroup {
  ChannelEventLoop next();
  ChannelPromise register(Channel channel);
}
