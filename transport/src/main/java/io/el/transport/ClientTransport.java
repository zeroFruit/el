package io.el.transport;

import io.el.channel.Channel;
import io.el.channel.ChannelHandler;
import io.el.channel.ChannelPipeline;
import io.el.channel.ChannelPromise;
import io.el.channel.DefaultChannelPromise;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

public class ClientTransport extends AbstractTransport<ClientTransport, Channel> {
  private ChannelHandler handler;

  public ChannelPromise connect(SocketAddress remoteAddress) {
    validate();

    final ChannelPromise registered = initAndRegister();
    final Channel channel = registered.channel();
    if (registered.cause() != null) {
      return registered;
    }

    ChannelPromise result = new DefaultChannelPromise(channel);
    if (registered.isDone()) {
      doConnect(remoteAddress, result);
      return result;
    }

    // When the flow reaches here, that means `registered` promise is still not
    // resolved, so add the promise listener for receiving callback.
    // And in the callback, Transport runs doConnect(...) to complete the binding
    registered.addListener(
        promise -> {
          if (promise.cause() != null) {
            result.setFailure(promise.cause());
            return;
          }
          doConnect(remoteAddress, result);
        });

    return result;
  }

  private void doConnect(final SocketAddress remoteAddress, final ChannelPromise connected) {
    final Channel channel = connected.channel();
    channel
        .channelEventLoop()
        .execute(
            () -> {
              channel
                  .connect(remoteAddress)
                  .addListener(
                      result -> {
                        if (result.isSuccess()) {
                          connected.setSuccess(null);
                          return;
                        }
                        connected.setFailure(result.cause());
                      });
            });
  }

  @Override
  void init(Channel channel) throws Exception {
    ChannelPipeline p = channel.pipeline();
    p.addLast(handler());
  }

  public ClientTransport handler(ChannelHandler handler) {
    this.handler = ObjectUtil.checkNotNull(handler, "handler");
    return this;
  }

  final ChannelHandler handler() {
    return handler;
  }

  /** Validate all the parameters */
  private ClientTransport validate() {
    if (handler == null) {
      throw new IllegalStateException("handler not set");
    }
    return this;
  }
}
