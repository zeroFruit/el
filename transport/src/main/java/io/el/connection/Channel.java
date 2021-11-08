package io.el.connection;

import io.el.concurrent.EventLoop;

public interface Channel extends ChannelOutboundInvoker {
    ChannelPipeline pipeline();

    boolean isRegistered();

    EventLoop eventLoop();
}
