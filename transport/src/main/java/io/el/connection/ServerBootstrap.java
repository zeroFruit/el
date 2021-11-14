package io.el.connection;

import io.el.concurrent.EventLoop;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ServerBootstrap<C extends Channel> {

  private ChannelEventLoopGroup parent;
  private ChannelEventLoopGroup child;

  private volatile ChannelHandler handler;

  private volatile ChannelFactory<? extends C> channelFactory;

  private static void doBind(
      final ChannelPromise regPromise, final Channel channel,
      final SocketAddress localAddress, ChannelPromise promise) {
    channel.channelEventLoop().execute(new Runnable() {
      @Override
      public void run() {
        if (regPromise.isSuccess()) {
          channel.bind(localAddress, promise).addListener(new ChannelPromiseListener() {
            @Override
            public void onComplete(ChannelPromise promise) throws Exception {
              // NO-OP
            }
          });
        } else {
          // error handling
        }
      }
    });
  }

  public ServerBootstrap<C> channel(Class<? extends C> channelClass) {
    return channelFactory(new ReflectiveChannelFactory<>(channelClass));
  }

  public ServerBootstrap<C> channelFactory(ChannelFactory<? extends C> channelFactory) {
    if (this.channelFactory != null) {
      throw new IllegalStateException("channelFactory set already");
    }
    this.channelFactory = channelFactory;
    return this;
  }

  public ServerBootstrap<C> group(ChannelEventLoopGroup parent, ChannelEventLoopGroup child) {
    this.parent = parent;
    if (this.child != null) {
      throw new IllegalStateException("childGroup already set");
    }
    this.child = child;
    return this;
  }

  public ServerBootstrap<C> childHandler(ChannelHandler childHandler) {
    this.handler = childHandler;
    return this;
  }

  public ChannelPromise bind(int inetPort) {
    return bind(new InetSocketAddress(inetPort));
  }

  public ChannelPromise bind(SocketAddress localAddress) {
    final ChannelPromise regPromise = initAndRegister();
    final Channel channel = regPromise.channel();

    final PendingRegisterationPromise promise = new PendingRegisterationPromise(channel);
    regPromise.addListener(new ChannelPromiseListener() {
      @Override
      public void onComplete(ChannelPromise cp) throws Exception {
        promise.registered();
        doBind(regPromise, channel, localAddress, promise);
      }
    });
    return promise;
  }

  final ChannelPromise initAndRegister() {
    Channel channel = channelFactory.newChannel();
    ChannelPipeline p = channel.pipeline();
    p.addLast(new ChannelInitializer<Channel>() {

      @Override
      protected void initChannel(Channel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (handler() != null) {
          pipeline.addLast(handler);
        }
        ch.channelEventLoop().execute(new Runnable() {
          @Override
          public void run() {
            pipeline.addLast(
                new ServerBootstrapAcceptor(child, handler));
          }
        });
      }
    });

    return parent.register(channel);
  }

  final ChannelHandler handler() {
    return handler;
  }

  private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

    private final ChannelEventLoopGroup childGroup;
    private final ChannelHandler childHandler;

    ServerBootstrapAcceptor(ChannelEventLoopGroup childGroup, ChannelHandler childHandler) {
      this.childGroup = childGroup;
      this.childHandler = childHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      final Channel child = (Channel) msg;

      child.pipeline().addLast(childHandler);

      childGroup.register(child).addListener(new ChannelPromiseListener() {
        @Override
        public void onComplete(ChannelPromise promise) throws Exception {
          if (!promise.isSuccess()) {
            // NO-OP
          }
        }
      });
    }
  }

  static final class PendingRegisterationPromise extends DefaultChannelPromise {

    private volatile boolean registered;

    public PendingRegisterationPromise(Channel channel) {
      super(channel);
    }

    void registered() {
      registered = true;
    }

    @Override
    protected EventLoop eventLoop() {
      if (registered) {
        return super.eventLoop();
      }
      return null;
    }
  }
}
