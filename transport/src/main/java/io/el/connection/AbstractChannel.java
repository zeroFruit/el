package io.el.connection;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractChannel implements Channel {

  private final DefaultChannelPipeline pipeline;
  private final Internal internal;
  private volatile ChannelEventLoop channelEventLoop;
  private volatile boolean registered;

  private volatile SocketAddress localAddress;
  private volatile SocketAddress remoteAddress;

  protected AbstractChannel() {
    pipeline = newChannelPipeline();
    internal = newInternal();
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
  public Internal internal() {
    return internal;
  }

  @Override
  public SocketAddress localAddress() {
    SocketAddress localAddress = this.localAddress;
    if (localAddress != null) {
      return localAddress;
    }
    try {
      this.localAddress = localAddress = internal().localAddress();
    } catch (Throwable t) {
      // TODO: error-handling
    }
    return localAddress;
  }

  @Override
  public SocketAddress remoteAddress() {
    SocketAddress remoteAddress = this.remoteAddress;
    if (remoteAddress != null) {
      return remoteAddress;
    }
    try {
      this.remoteAddress = remoteAddress = internal().remoteAddress();
    } catch (Throwable t) {
      // TODO: error-handling
    }
    return remoteAddress;
  }

  /**
   * Is called after the {@link Channel} is registered with its {@link ChannelEventLoop} as part of the register process.
   *
   * Sub-classes may override this method
   */
  protected void doRegister() throws Exception {
    // NO-OP
  }

  protected abstract void doBind(SocketAddress localAddress) throws Exception;

  protected abstract void doBeginRead() throws Exception;

  protected abstract void doWrite(ChannelOutboundBuffer in) throws Exception;

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

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return pipeline.bind(localAddress, promise);
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return pipeline.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public Channel read() {
    pipeline.read();
    return this;
  }

  @Override
  public ChannelOutboundInvoker write(Object msg, ChannelPromise promise) {
    return pipeline.write(msg, promise);
  }

  protected abstract AbstractInternal newInternal();

  protected abstract class AbstractInternal implements Internal {

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
        t.printStackTrace();
      }
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
    public void bind(SocketAddress localAddress, ChannelPromise promise) {
      assertEventLoop();

      if (!ensureOpen(promise)) {
        return;
      }

      boolean wasActive = isActive();
      try {
        doBind(localAddress);
      } catch (Throwable t) {
        promise.setFailure(t);
        // TODO: error-handling
        return;
      }

      if (!wasActive && isActive()) {
        invokeLater(new Runnable() {
          @Override
          public void run() {
            // pipeline.fireChannelActive();
          }
        });
      }
      promise.setSuccess(null);
    }

    private void assertEventLoop() {
      assert !registered || channelEventLoop.inEventLoop();
    }

    // TODO: figuring it out
    private void invokeLater(Runnable task) {
      try {
        // This method is used by outbound operation implementations to trigger an inbound event later.
        // They do not trigger an inbound event immediately because an outbound operation might have been
        // triggered by another inbound event handler method.  If fired immediately, the call stack
        // will look like this for example:
        //
        //   handlerA.inboundBufferUpdated() - (1) an inbound handler method closes a connection.
        //   -> handlerA.ctx.close()
        //      -> channel.unsafe.close()
        //         -> handlerA.channelInactive() - (2) another inbound handler method called while in (1) yet
        //
        // which means the execution of two inbound handler methods of the same handler overlap undesirably.
        channelEventLoop().execute(task);
      } catch (RejectedExecutionException e) {
        // TODO: error-handling
      }
    }

    @Override
    public SocketAddress remoteAddress() {
      return AbstractChannel.this.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
      return AbstractChannel.this.localAddress();
    }
  }
}
