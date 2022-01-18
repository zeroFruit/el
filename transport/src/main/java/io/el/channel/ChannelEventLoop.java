package io.el.channel;

import io.el.concurrent.EventLoop;

public interface ChannelEventLoop extends EventLoop, ChannelEventLoopGroup {
  ChannelEventLoopGroup parent();
}
