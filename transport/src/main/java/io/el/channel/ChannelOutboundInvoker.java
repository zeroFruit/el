package io.el.channel;

import java.net.SocketAddress;

public interface ChannelOutboundInvoker {

  /**
   * Request to bind to the given {@link SocketAddress} and notify the {@link ChannelPromise} once
   * the operation completes, either because the operation was successful or because of an error.
   */
  ChannelPromise bind(SocketAddress localAddress);
  /**
   * Request to bind to the given {@link SocketAddress} and notify the {@link ChannelPromise} once
   * the operation completes, either because the operation was successful or because of an error.
   *
   * <p>The result of the operation will be contained in {@link ChannelPromise}
   */
  ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise);

  /**
   * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify
   * the {@link ChannelPromise} once the operation completes, either because the operation was
   * successful or because of an error.
   *
   * <p>The result of the operation will be contained in {@link ChannelPromise}
   */
  ChannelPromise connect(
      SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);
}
