package io.el.channel;

import java.net.SocketAddress;

public interface ChannelOutboundInvoker {
  ChannelPromise bind(SocketAddress localAddress);
  ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise);
  ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);
}
