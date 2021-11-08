package io.el.connection;

import java.net.SocketAddress;

public interface ChannelOutboundInvoker {
    ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise);
}
