package io.el.connection;

import io.el.concurrent.EventLoop;

public interface EventLoopGroup {
    EventLoop next();

    ChannelPromise register(Channel channel);
}
