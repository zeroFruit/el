package io.el.channel;

import static io.el.channel.ChannelHandlerFlag.FLAG_OUTBOUND;
import static io.el.channel.ChannelHandlerFlag.flag;

import io.el.concurrent.EventLoop;
import io.el.internal.ObjectUtil;

abstract public class AbstractChannelHandlerContext implements ChannelHandlerContext {

  /**
   * {@link ChannelHandlerContext} of the next {@link ChannelHandler} contained in the {@link ChannelPipeline}
   * */
  volatile AbstractChannelHandlerContext next;

  /**
   * {@link ChannelHandlerContext} of the previous {@link ChannelHandler} contained in the {@link ChannelPipeline}
   * */
  volatile AbstractChannelHandlerContext prev;

  private final ChannelPipeline pipeline;
  private final String name;
  private final int executionFlag;
  final EventLoop eventLoop;

  AbstractChannelHandlerContext(String name, ChannelPipeline pipeline, EventLoop eventLoop,
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

  static void invokeExceptionCaught(final AbstractChannelHandlerContext next, final Throwable cause) {
    ObjectUtil.checkNotNull(cause, "cause");
    EventLoop eventLoop = next.eventLoop();
    if (eventLoop.inEventLoop()) {
      next.invokeExceptionCaught(cause);
    } else {
      eventLoop.execute(() -> next.invokeExceptionCaught(cause));
    }
  }

  private static boolean skipContext(AbstractChannelHandlerContext ctx, EventLoop eventLoop,
      int flag, int onlyFlag) {
    return ctx.eventLoop() == eventLoop && flag != onlyFlag;
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
    EventLoop currentEventLoop = eventLoop();
    do {
      ctx = ctx.next;
    } while (skipContext(ctx, currentEventLoop, executionFlag, FLAG_OUTBOUND));
    return ctx;
  }
}
