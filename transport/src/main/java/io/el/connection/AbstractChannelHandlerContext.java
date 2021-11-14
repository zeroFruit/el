package io.el.connection;

import io.el.concurrent.EventLoop;
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
