package io.el.channel;

import io.el.channel.Channel.Internal;
import io.el.concurrent.EventLoop;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;
import java.util.UUID;

/**
 * {@link DefaultChannelPipeline} manages a channel handler list. When an event occurs, it calls the
 * handlers it manages.
 */
public class DefaultChannelPipeline implements ChannelPipeline {

  private final Channel channel;
  private final HeadContext headContext;
  private final TailContext tailContext;
  private static final String HEAD_NAME = "HeaderContext";
  private static final String TAIL_NAME = "TailContext";

  public DefaultChannelPipeline(Channel channel) {
    this.channel = channel;
    this.tailContext = new TailContext(this);
    this.headContext = new HeadContext(this);

    this.tailContext.prev = this.headContext;
    this.headContext.next = this.tailContext;
  }

  @Override
  public Channel channel() {
    return this.channel;
  }

  @Override
  public ChannelPipeline addLast(ChannelHandler... handlers) {
    for (ChannelHandler handler : handlers) {
      this.addLast(handler);
    }
    return this;
  }

  private ChannelPipeline addLast(ChannelHandler handler) {
    final AbstractChannelHandlerContext prev = this.tailContext.prev;
    // TODO: generate name with the {@link ChannelHandler.getClass()}
    String name = UUID.randomUUID().toString();
    EventLoop eventLoop = null;
    final AbstractChannelHandlerContext newHandlerContext =
        new DefaultChannelHandlerContext(name, this, eventLoop, handler);

    this.tailContext.prev = newHandlerContext;
    newHandlerContext.next = this.tailContext;

    // TODO: add channel added callback to the list. And when channel is registered
    // to the eventloop, fire all pending added callback

    prev.next = newHandlerContext;
    newHandlerContext.prev = prev;
    return this;
  }

  @Override
  public ChannelPipeline remove(ChannelHandler handler) {
    final AbstractChannelHandlerContext context = this.context(handler);
    remove(context);
    return this;
  }

  private void remove(AbstractChannelHandlerContext context) {
    assert context != this.headContext && context != this.tailContext;
    atomicRemoveFromHeandlerList(context);
  }

  /** By using this, we can update the next and the prev reference atomically. */
  private synchronized void atomicRemoveFromHeandlerList(AbstractChannelHandlerContext context) {
    final AbstractChannelHandlerContext prev = context.prev;
    final AbstractChannelHandlerContext next = context.next;
    prev.next = next;
    next.prev = prev;
  }

  @Override
  public AbstractChannelHandlerContext context(ChannelHandler handler) {
    ObjectUtil.checkNotNull(handler, "handler");

    AbstractChannelHandlerContext ctx = headContext.next;
    for (; ; ) {
      if (ctx == null) {
        return null;
      }

      if (ctx.handler() == handler) {
        return ctx;
      }

      ctx = ctx.next;
    }
  }

  @Override
  public ChannelHandlerContext firstContext() {
    final AbstractChannelHandlerContext next = this.headContext.next;
    if (next == this.tailContext) {
      return null;
    }
    return next;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return this.tailContext.bind(localAddress);
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return this.tailContext.bind(localAddress, promise);
  }

  @Override
  public ChannelPromise connect(
      SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
    // TODO:
    return null;
  }

  @Override
  public ChannelPipeline fireChannelRegistered() {
    AbstractChannelHandlerContext.invokeChannelRegistered(headContext);
    return this;
  }

  @Override
  public ChannelInboundInvoker fireExceptionCaught(Throwable cause) {
    // TODO:
    return null;
  }

  /**
   * Todo: This class will extends AbstractChannelHandlerContext The first handler context.
   *
   * <p>For the events which calls handlers from last, the {@link HeadContext} will call channel's
   * methods after all the handers are called.
   */
  private static final class HeadContext extends AbstractChannelHandlerContext {

    private final HeadContextHandler handler;
    private final Internal internal;

    public HeadContext(DefaultChannelPipeline pipeline) {
      super(HEAD_NAME, pipeline, null, HeadContextHandler.class);
      this.internal = pipeline.channel().internal();
      this.handler = new HeadContextHandler(this.internal);
    }

    @Override
    public ChannelHandler handler() {
      return this.handler;
    }

    /** FIXME: This will be implemented in the {@link AbstractChannelHandlerContext} */
    @Override
    public ChannelPromise connect(
        SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      return null;
    }
  }

  private static final class HeadContextHandler
      implements ChannelOutboundHandler, ChannelInboundHandler {

    private final Internal internal;

    private HeadContextHandler(Internal internal) {
      this.internal = internal;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
        throws Exception {
      this.internal.bind(localAddress, promise);
    }

    @Override
    public void connect(
        ChannelHandlerContext ctx,
        SocketAddress remoteAddress,
        SocketAddress localAddress,
        ChannelPromise promise)
        throws Exception {
      // TODO:
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      // TODO: invoke channel registered callbacks of handlers
      ctx.fireChannelRegistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {}
  }

  private static final class TailContext extends AbstractChannelHandlerContext {

    private final TailContextHandler context;

    public TailContext(ChannelPipeline pipeline) {
      super(HEAD_NAME, pipeline, null, TailContextHandler.class);
      this.context = new TailContextHandler();
    }

    @Override
    public ChannelHandler handler() {
      return this.context;
    }

    /** FIXME: This will be implemented in the {@link AbstractChannelHandlerContext} */
    @Override
    public ChannelPromise connect(
        SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      return null;
    }
  }

  /** The last handler. */
  private static final class TailContextHandler implements ChannelInboundHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      // TODO: handle exception
    }
  }
}
