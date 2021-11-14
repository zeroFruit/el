package io.el.connection;

import io.el.concurrent.Promise;
import io.el.concurrent.PromiseListener;

public interface ChannelPromise extends Promise<Void> {

  Channel channel();

  @Override
  ChannelPromise addListener(PromiseListener<? extends Promise<? super Void>> listener);

  @Override
  ChannelPromise await() throws InterruptedException;
}
