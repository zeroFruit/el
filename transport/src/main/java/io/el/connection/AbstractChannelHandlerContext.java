package io.el.connection;

import static io.el.internal.ObjectUtil.checkNotNull;

import io.el.concurrent.EventLoop;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

abstract class AbstractChannelHandlerContext implements ChannelHandlerContext {

  volatile AbstractChannelHandlerContext next;
  volatile AbstractChannelHandlerContext prev;

  private final String name;
  private final DefaultChannelPipeline pipeline;

  private static final AtomicIntegerFieldUpdater<AbstractChannelHandlerContext> HANDLER_STATE_UPDATER =
      AtomicIntegerFieldUpdater.newUpdater(AbstractChannelHandlerContext.class, "handlerState");

  /**
   * Neither {@link ChannelHandler#handlerAdded(ChannelHandlerContext)} was called.
   */
  private static final int INIT = 0;
  /**
   * {@link ChannelHandler#handlerAdded(ChannelHandlerContext)} is about to be called.
   */
  private static final int ADD_PENDING = 1;
  /**
   * {@link ChannelHandler#handlerAdded(ChannelHandlerContext)} was called.
   */
  private static final int ADD_COMPLETE = 2;

  private volatile int handlerState = INIT;

  final EventLoop eventLoop;

  AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, EventLoop eventLoop,
      String name, Class<? extends ChannelHandler> handlerClass) {
    this.name = name;
    this.pipeline = pipeline;
    this.eventLoop = eventLoop;
  }

  static void invokeChannelRegistered(final AbstractChannelHandlerContext next) {
    EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeChannelRegistered();
      return;
    }
    eventLoop.execute(new Runnable() {
      @Override
      public void run() {
        next.invokeChannelRegistered();
      }
    });
  }

  @Override
  public Channel channel() {
    return pipeline.channel();
  }

  @Override
  public ChannelPipeline pipeline() {
    return pipeline;
  }

  @Override
  public EventLoop eventLoop() {
    if (eventLoop == null) {
      return channel().channelEventLoop();
    }
    return eventLoop;
  }

  @Override
  public ChannelHandlerContext fireChannelRegistered() {
    invokeChannelRegistered(next);
    return this;
  }

  @Override
  public ChannelHandlerContext fireChannelRead(Object msg) {
    return null;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return bind(localAddress, newPromise());
  }

  public ChannelPromise newPromise() {
    return new DefaultChannelPromise(channel(), eventLoop());
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    checkNotNull(localAddress, "localAddress");
    if (isNotValidPromise(promise)) {
      return promise;
    }

    final AbstractChannelHandlerContext next = findContextOutbound();
    // next = HeadContext
    EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeBind(localAddress, promise);
      return promise;
    }
    try {
//      next.invokeBind(localAddress, promise);
      eventLoop.execute(new Runnable() {
        @Override
        public void run() {
          next.invokeBind(localAddress, promise);
        }
      });
    } catch (Throwable cause) {
      promise.setFailure(cause);
    }
    return promise;
  }

  private boolean isNotValidPromise(ChannelPromise promise) {
    checkNotNull(promise, "promise");
    if (promise.isDone()) {
      throw new IllegalArgumentException("promise already done: " + promise);
    }
    if (promise.channel() != channel()) {
      throw new IllegalArgumentException(String.format(
          "promise.channel does not match: %s (expected: %s)", promise.channel(), channel()));
    }
    return false;
  }

  // TODO: implement me
  private AbstractChannelHandlerContext findContextInbound() {
    return this.next;
  }

  // TODO: implement me
  private AbstractChannelHandlerContext findContextOutbound() {
    AbstractChannelHandlerContext ctx = this;
    EventLoop currentEventLoop = eventLoop();
    do {
      if (ctx.prev == null) {
        return ctx;
      }
      ctx = ctx.prev;
    } while (skipContext(ctx, currentEventLoop));
    return ctx;
  }

  private static boolean skipContext(AbstractChannelHandlerContext ctx, EventLoop currentEventLoop) {
    return ctx.eventLoop() == currentEventLoop;
  }

  private void invokeBind(SocketAddress localAddress, ChannelPromise promise) {
    if (invokeHandler()) {
      try {
        ChannelHandler handler = handler();
        ((ChannelOutboundHandler) handler).bind(this, localAddress, promise);
      } catch (Throwable t) {
        // TODO: error-handling
        t.printStackTrace();
      }
    } else {
      bind(localAddress, promise);
    }
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return null;
  }

  @Override
  public ChannelHandlerContext read() {
    return null;
  }

  @Override
  public ChannelOutboundInvoker write(Object msg, ChannelPromise promise) {
    return null;
  }

  final void setAddPending() {
    boolean updated = HANDLER_STATE_UPDATER.compareAndSet(this, INIT, ADD_PENDING);
    assert updated; // This should always be true as it MUST be called before setAddComplete() or setRemoved().
  }

  final boolean setAddComplete() {
    while (true) {
      int oldState = handlerState;
      if (HANDLER_STATE_UPDATER.compareAndSet(this, oldState, ADD_COMPLETE)) {
        return true;
      }
    }
  }

  final void callHandlerAdded() throws Exception {
    // We must call setAddComplete before calling handlerAdded. Otherwise if the handlerAdded method generates
    // any pipeline events ctx.handler() will miss them because the state will not allow it.
    if (setAddComplete()) {
      handler().handlerAdded(this);
    }
  }

  private void invokeChannelRegistered() {
    if (!invokeHandler()) {
      fireChannelRegistered();
    }
    try {
      ((ChannelInboundHandler) handler()).channelRegistered(this);
    } catch (Throwable t) {
      // TODO: error-handling
    }
  }

  /**
   * Makes best possible effort to detect if {@link ChannelHandler#handlerAdded(ChannelHandlerContext)} was called
   * yet. If not return {@code false} and if called or could not detect return {@code true}.
   *
   * If this method returns {@code false} we will not invoke the {@link ChannelHandler} but just forward the event.
   * This is needed as {@link DefaultChannelPipeline} may already put the {@link ChannelHandler} in the linked-list
   * but not called {@link ChannelHandler#handlerAdded(ChannelHandlerContext)}.
   */
  private boolean invokeHandler() {
    int handlerState = this.handlerState;
    return handlerState == ADD_COMPLETE || handlerState == ADD_PENDING;
  }
}
