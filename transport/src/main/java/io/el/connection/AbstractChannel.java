package io.el.connection;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

public abstract class AbstractChannel implements Channel {

  private final DefaultChannelPipeline pipeline;
  private volatile ChannelEventLoop channelEventLoop;
  private volatile boolean registered;

  protected AbstractChannel() {
    pipeline = newChannelPipeline();
  }

  protected DefaultChannelPipeline newChannelPipeline() {
    return new DefaultChannelPipeline(this);
  }

  @Override
  public ChannelPipeline pipeline() {
    return pipeline;
  }

  @Override
  public ChannelEventLoop channelEventLoop() {
    ChannelEventLoop channelEventLoop = this.channelEventLoop;
    if (channelEventLoop == null) {
      throw new IllegalStateException("channel not registered to an event loop");
    }
    return channelEventLoop;
  }

  @Override
  public boolean isRegistered() {
    return registered;
  }

  @Override
  public void register(ChannelEventLoop channelEventLoop, ChannelPromise promise) {
    if (isRegistered()) {
      promise.setFailure(new IllegalStateException("registered to an event loop already"));
      return;
    }
    AbstractChannel.this.channelEventLoop = channelEventLoop;

    if (channelEventLoop.inEventLoop()) {
      register(promise);
      return;
    }
    try {
      channelEventLoop.execute(new Runnable() {
        @Override
        public void run() {
          register(promise);
        }
      });
    } catch (Throwable t) {
      // TODO: error-handling
    }
  }

  /**
   * Is called after the {@link Channel} is registered with its {@link ChannelEventLoop} as part of the register process.
   *
   * Sub-classes may override this method
   */
  protected void doRegister() throws Exception {
    // NO-OP
  }

  protected final boolean ensureOpen(ChannelPromise promise) {
    if (isOpen()) {
      return true;
    }
    if (promise.isDone()) {
      // TODO: leave a log
      return false;
    }
    promise.setFailure(new ClosedChannelException());
    return false;
  }

  private void register(ChannelPromise promise) {
    try {
      if (!ensureOpen(promise)) {
        return;
      }
      doRegister();
      registered = true;

      // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the
      // user may already fire events through the pipeline in the ChannelFutureListener.
      pipeline.invokeHandlerAddedIfNeeded();
      pipeline.fireChannelRegistered();
      promise.setSuccess(null);
    } catch (Throwable t) {
      // TODO: error-handling
    }
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return null;
  }
}
