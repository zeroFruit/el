package io.el.channel;

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
    this.tailContext = new TailContext();
    this.headContext = new HeadContext();

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
    final AbstractChannelHandlerContext newHandlerContext = new DefaultHandlerContext__(name, this, eventLoop, handler);


    this.tailContext.prev = newHandlerContext;
    newHandlerContext.next = this.tailContext;

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

  /**
   * By using this, we can update the next and the prev reference atomically.
   */
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
    for (;;) {
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

  @Override
  public ChannelInboundInvoker fireExceptionCaught(Throwable cause) {
    return null;
  }

  private static final class DefaultHandlerContext__ extends AbstractChannelHandlerContext {

    private final ChannelHandler handler;

    public DefaultHandlerContext__(String name, ChannelPipeline pipeline, EventLoop eventLoop,
        ChannelHandler handler) {
      super(name, pipeline, eventLoop, handler.getClass());
      this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
      return this.handler;
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
  }

  /**
   * Todo: This class will extends AbstractChannelHandlerContext
   * The first handler context.
   * <p>
   * For the events which calls handlers from last,
   * the {@link HeadContext} will call channel's methods
   * after all the handers are called.
   */
  private static final class HeadContext extends AbstractChannelHandlerContext {

    private final HeadContextHandler context;

    public HeadContext(DefaultChannelPipeline pipeline) {
      super(HEAD_NAME, pipeline, null, HeadContextHandler.class);
      this.context = new HeadContextHandler();
    }

    @Override
    public ChannelHandler handler() {
      return this.context;
    }

    /**
     * FIXME: This will be implemented in the {@link AbstractChannelHandlerContext}
     */
    @Override
    public ChannelPromise bind(SocketAddress localAddress) {
      return null;
    }

    /**
     * Pipeline will call `bind` from the tail handler. This is the last `bind` function in the pipe
     * line.
     */
    @Override
    public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
      this.channel().internal().bind(localAddress, promise);
      // FIXME: I'm not sure whether returning the promise from argument is ok or not
      return promise;
    }

    /**
     * FIXME: This will be implemented in the {@link AbstractChannelHandlerContext}
     */
    @Override
    public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      return null;
    }
  }

  private static final class HeadContextHandler implements ChannelHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }
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

    /**
     * FIXME: This will be implemented in the {@link AbstractChannelHandlerContext}
     */
    @Override
    public ChannelPromise bind(SocketAddress localAddress) {
      return null;
    }

    /**
     * FIXME: This will be implemented in the {@link AbstractChannelHandlerContext}
     */
    @Override
    public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
      return null;
    }

    /**
     * FIXME: This will be implemented in the {@link AbstractChannelHandlerContext}
     */
    @Override
    public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      return null;
    }
  }

  /**
   * The last handler.
   */
  private static final class TailContextHandler implements ChannelHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }
  }
}
