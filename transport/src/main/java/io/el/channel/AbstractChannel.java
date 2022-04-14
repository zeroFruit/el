package io.el.channel;

import java.net.SocketAddress;

public abstract class AbstractChannel implements Channel {
  private final ChannelId id;
  private final ChannelPipeline pipeline;
  private final Internal internal;

  private ChannelEventLoop channelEventLoop;

  protected AbstractChannel(ChannelId id) {
    this.id = id;
    this.pipeline = null;
    this.internal = null;
  }

  @Override
  public final ChannelId id() {
    return id;
  }

  @Override
  public ChannelPipeline pipeline() {
    return pipeline;
  }

  @Override
  public Internal internal() {
    return internal;
  }

  @Override
  public ChannelEventLoop channelEventLoop() {
    return channelEventLoop;
  }

  @Override
  public SocketAddress localAddress() {
    return internal().localAddress();
  }

  @Override
  public SocketAddress remoteAddress() {
    return internal().remoteAddress();
  }

  @Override
  public ChannelPromise register(ChannelEventLoop eventLoop) {
    ChannelPromise promise = newPromise();
    internal().register(eventLoop, promise);
    return promise;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return pipeline().bind(localAddress, newPromise());
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress) {
    return pipeline().connect(remoteAddress, localAddress(), newPromise());
  }

  private ChannelPromise newPromise() {
    return new DefaultChannelPromise(this, channelEventLoop);
  }

  protected abstract class AbstractInternal implements Internal {
    // localAddress, remoteAddress should be implemented in
    // concrete Internal class

    @Override
    public void register(ChannelEventLoop eventLoop, ChannelPromise promise) {
      // TODO: implement me
    }

    @Override
    public void bind(SocketAddress localAddress, ChannelPromise promise) {
      // TODO: implement me
    }

    @Override
    public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
      // TODO: implement me
    }
  }
}
