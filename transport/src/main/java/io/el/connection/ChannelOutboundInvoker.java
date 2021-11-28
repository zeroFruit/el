package io.el.connection;

import java.net.SocketAddress;

public interface ChannelOutboundInvoker {

  ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise);

  ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

  ChannelOutboundInvoker read();

  ChannelOutboundInvoker write(Object msg, ChannelPromise promise);
}
