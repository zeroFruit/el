package io.el.channel;

import java.net.SocketAddress;

public interface Channel {

  /** Returns the unique identifier of this {@link Channel} */
  ChannelId id();

  /** Returns the assigned {@link ChannelPipeline} */
  ChannelPipeline pipeline();

  /** Returns {@code true} if the {@link Channel} is open and may get active later */
  boolean isOpen();

  /** Returns {@code true} if the {@link Channel} is registered with an {@link ChannelEventLoop} */
  boolean isRegistered();

  /** Returns {@code true} if the {@link Channel} is active and so connected. */
  boolean isActive();

  ChannelEventLoop channelEventLoop();

  /**
   * Returns the local address where this channel is bound to. {@link SocketAddress} is supposed to
   * be down-cast into more concrete type such as {@link java.net.InetSocketAddress} to retrieve the
   * detailed info
   */
  SocketAddress localAddress();

  /**
   * Returns the remote address where this channel is connected to. The returned {@link
   * SocketAddress} is supposed to be down-cast into more concrete type such as {@link
   * java.net.InetSocketAddress} to retrieve the detailed info
   */
  SocketAddress remoteAddress();

  ChannelPromise bind(SocketAddress localAddress);

  ChannelPromise connect(SocketAddress remoteAddress);

  /** Returns an internal-use-only object that providees unsafe operations */
  Internal internal();

  interface Internal {

    /** Return the {@link SocketAddress} to which is bound local or {@code null} if none. */
    SocketAddress localAddress();

    /**
     * Return the {@link SocketAddress} to which is bound remote or {@code null} if none is bound
     * yet.
     */
    SocketAddress remoteAddress();

    /**
     * Register the {@link Channel} of the {@link ChannelPromise} and notify the {@link
     * ChannelPromise} once the registration was complete.
     */
    void register(ChannelEventLoop eventLoop, ChannelPromise promise);

    /**
     * Bind the {@link SocketAddress} to the {@link Channel} of the {@link ChannelPromise} and
     * notify it once its done
     */
    void bind(SocketAddress localAddress, ChannelPromise promise);

    /**
     * Connect the {@link Channel} of the given {@link ChannelPromise} with the given remote {@link
     * SocketAddress}.
     */
    void connect(SocketAddress remoteAddress, ChannelPromise promise);
  }
}
