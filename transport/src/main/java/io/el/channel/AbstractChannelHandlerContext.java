package io.el.channel;

import static io.el.channel.ChannelHandlerFlag.flag;
import static io.el.channel.ChannelHandlerFlag.isNotInbound;
import static io.el.channel.ChannelHandlerFlag.isNotOutbound;

import io.el.concurrent.EventLoop;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

public abstract class AbstractChannelHandlerContext implements ChannelHandlerContext {

  /**
   * {@link ChannelHandlerContext} of the next {@link ChannelHandler} contained in the {@link
   * ChannelPipeline}
   */
  volatile AbstractChannelHandlerContext next;

  /**
   * {@link ChannelHandlerContext} of the previous {@link ChannelHandler} contained in the {@link
   * ChannelPipeline}
   */
  volatile AbstractChannelHandlerContext prev;

  private final ChannelPipeline pipeline;
  private final String name;
  private final int executionFlag;
  final EventLoop eventLoop;

  AbstractChannelHandlerContext(
      String name,
      ChannelPipeline pipeline,
      EventLoop eventLoop,
      Class<? extends ChannelHandler> handlerClass) {
    this.name = ObjectUtil.checkNotNull(name, "name");
    this.pipeline = pipeline;
    this.executionFlag = flag(handlerClass);
    this.eventLoop = eventLoop;
  }

  static void invokeChannelRegistered(final AbstractChannelHandlerContext next) {
    EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeChannelRegistered();
    } else {
      eventLoop.execute(next::invokeChannelRegistered);
    }
  }

  static void invokeExceptionCaught(
      final AbstractChannelHandlerContext next, final Throwable cause) {
    ObjectUtil.checkNotNull(cause, "cause");
    EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeExceptionCaught(cause);
    } else {
      eventLoop.execute(() -> next.invokeExceptionCaught(cause));
    }
  }

  @Override
  public String name() {
    return name;
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
    invokeChannelRegistered(findContextInbound());
    return this;
  }

  private void invokeChannelRegistered() {
    try {
      ((ChannelInboundHandler) handler()).channelRegistered(this);
    } catch (Throwable t) {
      invokeExceptionCaught(t);
    }
  }

  @Override
  public ChannelHandlerContext fireExceptionCaught(Throwable t) {
    invokeExceptionCaught(findContextInbound(), t);
    return this;
  }

  private void invokeExceptionCaught(Throwable t) {
    try {
      ((ChannelInboundHandler) handler()).exceptionCaught(this, t);
    } catch (Throwable err) {
      // TODO: leave warning logs
    }
  }

  private AbstractChannelHandlerContext findContextInbound() {
    AbstractChannelHandlerContext ctx = this;
    do {
      ctx = ctx.next;
    } while (isNotInbound(ctx.executionFlag));
    return ctx;
  }

  private AbstractChannelHandlerContext findContextOutbound() {
    AbstractChannelHandlerContext ctx = this;
    do {
      ctx = ctx.prev;
    } while (isNotOutbound(ctx.executionFlag));
    return ctx;
  }

  public ChannelPromise newPromise() {
    return new DefaultChannelPromise(channel(), eventLoop());
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return this.bind(localAddress, newPromise());
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    ObjectUtil.checkNotNull(localAddress, "localAddress");
    if (isNotValidPromise(promise)) {
      // canceled
      return promise;
    }

    final AbstractChannelHandlerContext next = findContextOutbound();
    final EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeBind(localAddress, promise);
    } else {
      safeExecute(
          eventLoop,
          () -> {
            next.invokeBind(localAddress, promise);
          },
          promise);
    }
    return promise;
  }

  private void invokeBind(SocketAddress localAddress, ChannelPromise promise) {
    try {
      ((ChannelOutboundHandler) handler()).bind(this, localAddress, promise);
    } catch (Throwable t) {
      promise.setFailure(t);
    }
  }

  private static boolean safeExecute(
      EventLoop eventLoop, Runnable runnable, ChannelPromise promise) {
    try {
      eventLoop.execute(runnable);
      return true;
    } catch (Throwable cause) {
      promise.setFailure(cause);
      return false;
    }
  }

  private boolean isNotValidPromise(ChannelPromise promise) {
    ObjectUtil.checkNotNull(promise, "promise");

    if (promise.isDone()) {
      // if the promise is canceled, we do not need to continue the code
      if (promise.isCancelled()) {
        return true;
      }
      throw new IllegalArgumentException("promise already done: " + promise);
    }

    // ChannelHandlerContext receives a ChannelPromise to notify the event result to the listeners.
    // If the promise.channel is not the same as the ChannelHandlerContext, the listeners
    // will receive a wrong channel that is not related to the event.
    if (promise.channel() != channel()) {
      throw new IllegalArgumentException(
          String.format(
              "promise.channel does not match: %s (expected: %s)", promise.channel(), channel()));
    }

    if (promise.getClass() == DefaultChannelPromise.class) {
      return false;
    }
    return false;
  }

  @Override
  public ChannelPromise connect(
      SocketAddress remoteAddress,
      SocketAddress localAddress,
      // TODO: implement me
      ChannelPromise promise) {
    return null;
  }
}
