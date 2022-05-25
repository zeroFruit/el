package io.el.channel;

import io.el.concurrent.DefaultPromise;
import io.el.concurrent.EventLoop;
import io.el.concurrent.Promise;
import io.el.concurrent.PromiseListener;

public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise {

  private final Channel channel;

  public DefaultChannelPromise(Channel channel, EventLoop eventLoop) {
    super(eventLoop, () -> {});
    this.channel = channel;
  }

  @Override
  public Promise<Void> setSuccess(Void result) {
    super.setSuccess(result);
    return this;
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
