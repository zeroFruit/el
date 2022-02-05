package io.el.channel;

import java.net.SocketAddress;

public class DefaultChannelPipeline implements ChannelPipeline {

  private final Channel channel;

  public DefaultChannelPipeline(Channel channel) {
    this.channel = channel;
  }

  @Override
  public Channel channel() {
    return this.channel;
  }

  @Override
  public ChannelPipeline addLast(ChannelHandler... handlers) {
    return null;
  }

  @Override
  public ChannelPipeline remove(ChannelHandler handler) {
    return null;
  }

  @Override
  public ChannelHandlerContext context(ChannelHandler handler) {
    return null;
  }

  @Override
  public ChannelHandlerContext firstContext() {
    return null;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return null;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelPipeline fireChannelRegistered() {
    return null;
  }
}
