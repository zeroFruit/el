package io.el.channel;

import io.el.concurrent.DefaultPromise;
import io.el.concurrent.EventLoop;
import io.el.concurrent.Promise;
import io.el.concurrent.PromiseListener;

public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise {

  private final Channel channel;

  public DefaultChannelPromise(Channel channel) {
    super(null, () -> {});
    this.channel = channel;
  }

  public DefaultChannelPromise(Channel channel, EventLoop eventLoop) {
    super(eventLoop, () -> {});
    this.channel = channel;
  }

  @Override
  public ChannelPromise setSuccess(Void result) {
    super.setSuccess(result);
    return this;
  }

  @Override
  public ChannelPromise setFailure(Throwable cause) {
    super.setFailure(cause);
    return this;
  }

  @Override
  public Throwable cause() {
    return super.cause();
  }

  @Override
  public Channel channel() {
    return channel;
  }

  @Override
  public ChannelPromise addListener(PromiseListener<? extends Promise<? super Void>> listener) {
    super.addListener(listener);
    return this;
  }

  @Override
  public ChannelPromise await() throws InterruptedException {
    super.await();
    return this;
  }
}
