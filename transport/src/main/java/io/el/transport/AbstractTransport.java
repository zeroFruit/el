package io.el.transport;

import io.el.channel.Channel;
import io.el.channel.ChannelEventLoopGroup;
import io.el.channel.ChannelFactory;
import io.el.channel.ChannelPromise;
import io.el.channel.DefaultChannelPromise;
import io.el.channel.ReflectiveChannelFactory;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

public abstract class AbstractTransport<T extends AbstractTransport<T, C>, C extends Channel> {
  private volatile ChannelEventLoopGroup group;

  private volatile ChannelFactory<C> channelFactory;
  private volatile SocketAddress localAddress;

  public T group(ChannelEventLoopGroup group) {
    ObjectUtil.checkNotNull(group, "group");
    if (this.group != null) {
      throw new IllegalStateException("group set already");
    }
    this.group = group;
    return self();
  }

  public T channel(Class<? extends C> channelClass) {
    this.channelFactory =
        new ReflectiveChannelFactory<C>(ObjectUtil.checkNotNull(channelClass, "channelClass"));
    return self();
  }

  public ChannelPromise bind(SocketAddress localAddress) {
    validate();
    final ChannelPromise registered = initAndRegister();
    final Channel channel = registered.channel();
    if (registered.cause() != null) {
      return registered;
    }
    ChannelPromise result = new DefaultChannelPromise(channel);
    if (registered.isDone()) {
      doBind(registered, channel, localAddress, result);
      return result;
    }
    // When the flow reaches here, that means `registered` promise is still not
    // resolved, so add the promise listener for receiving callback.
    // And in the callback, Transport runs doBind(...) to complete the binding
    registered.addListener(
        promise -> {
          if (!promise.isSuccess()) {
            result.setFailure(promise.cause());
            return;
          }
          doBind(registered, channel, localAddress, result);
        });

    return result;
  }

  public T localAddress(SocketAddress localAddress) {
    this.localAddress = localAddress;
    return self();
  }

  final ChannelPromise initAndRegister() {
    Channel channel = channelFactory.newChannel();
    try {
      init(channel);
    } catch (Throwable t) {
      return new DefaultChannelPromise(channel).setFailure(t);
    }
    return group.register(channel);
  }

  private void doBind(
      final ChannelPromise registered,
      final Channel channel,
      final SocketAddress localAddress,
      final ChannelPromise result) {
    channel
        .channelEventLoop()
        .execute(
            () -> {
              if (registered.isSuccess()) {
                channel
                    .bind(localAddress)
                    .addListener(
                        promise -> {
                          if (promise.isSuccess()) {
                            result.setSuccess(null);
                            return;
                          }
                          result.setFailure(promise.cause());
                        });
                return;
              }
              result.setFailure(registered.cause());
            });
  }

  abstract void init(Channel channel) throws Exception;

  /** Validate all the parameters */
  private T validate() {
    if (group == null) {
      throw new IllegalStateException("group not set");
    }
    if (channelFactory == null) {
      throw new IllegalStateException("channel or channelFactory not set");
    }
    return self();
  }

  private T self() {
    return (T) this;
  }
}
