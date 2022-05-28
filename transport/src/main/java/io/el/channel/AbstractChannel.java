package io.el.channel;

import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

public abstract class AbstractChannel implements Channel {
  private final ChannelId id;
  private final ChannelPipeline pipeline;
  private final Internal internal;

  private volatile ChannelEventLoop channelEventLoop;
  private volatile boolean registered;

  protected AbstractChannel(ChannelId id) {
    this.id = id;
    this.internal = newInternal(); // internal is used in the pipeline
    this.pipeline = newPipeline();
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

  public boolean isRegistered() {
    return registered;
  }

  protected ChannelPipeline newPipeline() {
    return new DefaultChannelPipeline(this);
  }

  /**
   * Create a new {@link AbstractInternal} instance which will be used for the life-time of the
   * {@link Channel}
   */
  protected abstract AbstractInternal newInternal();

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

  protected abstract void doRegister();

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return pipeline().bind(localAddress, newPromise());
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress) {
    return pipeline().connect(remoteAddress, newPromise());
  }

  public boolean isBound(SocketAddress localAddress) {
    return this.localAddress().equals(localAddress);
  }

  private ChannelPromise newPromise() {
    return new DefaultChannelPromise(this, channelEventLoop);
  }

  protected abstract class AbstractInternal implements Internal {
    // localAddress, remoteAddress should be implemented in
    // concrete Internal class

    @Override
    public void register(ChannelEventLoop eventLoop, ChannelPromise promise) {
      ObjectUtil.checkNotNull(eventLoop, "channelEventLoop");
      if (isRegistered()) {
        promise.setFailure(new IllegalStateException("already registered to a channel event loop"));
        return;
      }
      AbstractChannel.this.channelEventLoop = eventLoop;

      if (eventLoop.inEventLoop()) {
        doRegister(promise);
        return;
      }

      try {
        eventLoop.execute(() -> doRegister(promise));
      } catch (Throwable t) {
        // TODO: logging, error handling
        promise.setFailure(t);
      }
    }

    private void doRegister(ChannelPromise promise) {
      try {
        registered = true;
        pipeline.fireChannelRegistered();
        AbstractChannel.this.doRegister();
        promise.setSuccess(null);
      } catch (Throwable t) {
        // TODO: logging, error handling
        promise.setFailure(t);
      }
    }

    @Override
    public void bind(SocketAddress localAddress, ChannelPromise promise) {
      ObjectUtil.checkNotNull(localAddress, "channelLocalAddress");

      if (isRegistered() && !channelEventLoop.inEventLoop()) {
        promise.setFailure(new IllegalStateException("channel state is not able to bind"));
        return;
      }
      if (isBound(localAddress)) {
        promise.setFailure(new IllegalStateException("local address is already bound"));
        return;
      }

      try {
        this.doBind(localAddress, promise);
      } catch (Exception e) {
        promise.setFailure(e);
      }
    }

    @Override
    public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
      // TODO: implement me
    }

    public abstract void doBind(SocketAddress localAddress, ChannelPromise promise);
  }
}
