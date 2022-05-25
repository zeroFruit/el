package io.el.channel;

import io.el.concurrent.EventLoopGroup;

public interface ChannelEventLoopGroup extends EventLoopGroup {

  /** Returns {@link ChannelEventLoop} it manages */
  ChannelEventLoop next();

  /** Bind {@link Channel} with the {@link ChannelEventLoop} one of it manages */
  ChannelPromise register(Channel channel);
}
