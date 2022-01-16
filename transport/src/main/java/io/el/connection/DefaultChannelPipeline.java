package io.el.connection;

import io.el.concurrent.EventLoop;
import io.el.concurrent.EventLoopGroup;
import io.el.connection.Channel.Internal;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;

public class DefaultChannelPipeline implements ChannelPipeline {

  private static final String HEAD_NAME = HeadContext.class.getSimpleName();
  private static final String TAIL_NAME = TailContext.class.getSimpleName();

  private final Channel channel;
  final AbstractChannelHandlerContext head;
  final AbstractChannelHandlerContext tail;

  private boolean firstRegistration = true;

  /**
   * This is the head of a linked list that is processed by
   * all the pending {@link #callHandlerAdded(AbstractChannelHandlerContext)}.
   *
   * We only keep the head because it is expected that the list is used infrequently and its size is small.
   * Thus full iterations to do insertions is assumed to be a good compromised to saving memory and tail management
   * complexity.
   */
  private PendingHandlerCallback pendingHandlerCallbackHead;
  /**
   * Set to {@code true} once the {@link AbstractChannel} is registered.Once set to {@code true} the value will never
   * change.
   */
  private boolean registered;

  protected DefaultChannelPipeline(Channel channel) {
    this.channel = channel;
    tail = new TailContext(this);
    head = new HeadContext(this);

    head.next = tail;
    tail.prev = head;
  }

  @Override
  public ChannelPipeline addLast(ChannelHandler... handlers) {
    for (ChannelHandler h: handlers) {
      if (h == null) {
        break;
      }
      addLast(null, null, h);
    }
    return this;
  }

  @Override
  public ChannelPipeline remove(ChannelHandler handler) {
    AbstractChannelHandlerContext ctx = (AbstractChannelHandlerContext) context(handler);
    assert ctx != head && ctx != tail;

    synchronized (this) {
      atomicRemoveFromHandlerList(ctx);
      // FIXME: when add handlerRemoved event-handler, then fire handler-removed event
    }
    return this;
  }

  private synchronized void atomicRemoveFromHandlerList(AbstractChannelHandlerContext ctx) {
    AbstractChannelHandlerContext prev = ctx.prev;
    AbstractChannelHandlerContext next = ctx.next;
    prev.next = next;
    next.prev = prev;
  }

  public final ChannelPipeline addLast(EventLoopGroup group, String name, ChannelHandler handler) {
    final AbstractChannelHandlerContext newCtx;
    synchronized (this) {
      newCtx = newContext(group, name, handler);

      AbstractChannelHandlerContext prev = tail.prev;
      newCtx.prev = prev;
      newCtx.next = tail;
      prev.next = newCtx;
      tail.prev = newCtx;

      // If the registered is false it means that the channel was not registered on an eventLoop yet.
      // In this case we add the context to the pipeline and add a task that will call
      // ChannelHandler.handlerAdded(...) once the channel is registered.
      if (!registered) {
        newCtx.setAddPending();
        callHandlerCallbackLater(newCtx);
        return this;
      }

      EventLoop eventLoop = newCtx.eventLoop();
      if (!eventLoop.inEventLoop()) {
        callHandlerAddedInEventLoop(newCtx, eventLoop);
        return this;
      }
    }
    callHandlerAdded(newCtx);
    return this;
  }

  @Override
  public ChannelPipeline fireChannelRegistered() {
    AbstractChannelHandlerContext.invokeChannelRegistered(head);
    return this;
  }

  @Override
  public ChannelPipeline fireChannelRead(Object msg) {
    return null;
  }

  @Override
  public ChannelHandlerContext firstContext() {
    AbstractChannelHandlerContext first = head.next;
    if (first == tail) {
      return null;
    }
    return head.next;
  }

  @Override
  public ChannelHandlerContext context(ChannelHandler handler) {
    ObjectUtil.checkNotNull(handler, "handler");

    AbstractChannelHandlerContext ctx = head.next;
    while (true) {
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
  public Channel channel() {
    return channel;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return tail.bind(localAddress);
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return tail.bind(localAddress, promise);
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelOutboundInvoker read() {
    return null;
  }

  @Override
  public ChannelOutboundInvoker write(Object msg, ChannelPromise promise) {
    return null;
  }

  final void invokeHandlerAddedIfNeeded() {
    assert channel.channelEventLoop().inEventLoop();
    if (!firstRegistration) {
      return;
    }
    firstRegistration = false;
    // We are now registered to the EventLoop. It's time to call the callbacks for the ChannelHandlers,
    // that were added before the registration was done.
    callHandlerAddedForAllHandlers();
  }

  private AbstractChannelHandlerContext newContext(EventLoopGroup group, String name, ChannelHandler handler) {
    return new DefaultChannelHandlerContext(this, childEventLoop(group), name, handler);
  }

  private EventLoop childEventLoop(EventLoopGroup group) {
    if (group == null) {
      return null;
    }
    return group.next();
  }

  private void callHandlerCallbackLater(AbstractChannelHandlerContext ctx) {
    assert !registered;

    PendingHandlerCallback task = new PendingHandlerAddedTask(ctx);
    PendingHandlerCallback pending = pendingHandlerCallbackHead;
    if (pending == null) {
      pendingHandlerCallbackHead = task;
      return;
    }
    while (pending.next != null) {
      pending = pending.next;
    }
    pending.next = task;
  }

  private void callHandlerAdded(final AbstractChannelHandlerContext ctx) {
    try {
      ctx.callHandlerAdded();
    } catch (Throwable t) {
      // TODO: error handling
    }
  }

  private void callHandlerAddedInEventLoop(final AbstractChannelHandlerContext newCtx, EventLoop eventLoop) {
    newCtx.setAddPending();
    eventLoop.execute(new Runnable() {
      @Override
      public void run() {
        callHandlerAdded(newCtx);
      }
    });
  }

  private void callHandlerAddedForAllHandlers() {
    final PendingHandlerCallback pendingHandlerCallbackHead;
    synchronized (this) {
      assert !registered;

      // This Channel itself was registered.
      registered = true;

      pendingHandlerCallbackHead = this.pendingHandlerCallbackHead;
      // Null out so it can be GC'ed.
      this.pendingHandlerCallbackHead = null;
    }

    // This must happen outside of the synchronized(...) block as otherwise handlerAdded(...) may be called while
    // holding the lock and so produce a deadlock if handlerAdded(...) will try to add another handler from outside
    // the EventLoop.
    PendingHandlerCallback task = pendingHandlerCallbackHead;
    while (task != null) {
      task.execute();
      task = task.next;
    }
  }

  final class TailContext extends AbstractChannelHandlerContext
      implements ChannelInboundHandler {

    TailContext(DefaultChannelPipeline pipeline) {
      super(pipeline, null, TAIL_NAME, TailContext.class);
      setAddComplete();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      // TODO:
    }

    @Override
    public ChannelHandler handler() {
      return this;
    }
  }

  final class HeadContext extends AbstractChannelHandlerContext
      implements ChannelOutboundHandler, ChannelInboundHandler {

    private final Internal internal;

    HeadContext(DefaultChannelPipeline pipeline) {
      super(pipeline, null, HEAD_NAME, HeadContext.class);
      this.internal = pipeline.channel().internal();
      setAddComplete();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      invokeHandlerAddedIfNeeded();
      ctx.fireChannelRegistered();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
        throws Exception {
      internal.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
        SocketAddress localAddress, ChannelPromise promise) throws Exception {
      internal.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
      // TODO: channel.beginRead();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
        throws Exception {
      // TODO: channel.write();
    }

    @Override
    public ChannelHandler handler() {
      return this;
    }
  }

  private abstract static class PendingHandlerCallback implements Runnable {
    final AbstractChannelHandlerContext ctx;
    PendingHandlerCallback next;

    PendingHandlerCallback(AbstractChannelHandlerContext ctx) {
      this.ctx = ctx;
    }

    abstract void execute();
  }

  private final class PendingHandlerAddedTask extends PendingHandlerCallback {

    PendingHandlerAddedTask(AbstractChannelHandlerContext ctx) {
      super(ctx);
    }

    @Override
    public void run() {
      callHandlerAdded(ctx);
    }

    @Override
    void execute() {
      EventLoop eventLoop = ctx.eventLoop();
      if (eventLoop.inEventLoop()) {
        callHandlerAdded(ctx);
        return;
      }
      try {
        eventLoop.execute(this);
      } catch (RejectedExecutionException e) {
        // TODO: error-handling
      }
    }
  }
}
