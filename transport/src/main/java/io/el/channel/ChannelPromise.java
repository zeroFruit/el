package io.el.channel;

import io.el.concurrent.Promise;
import io.el.concurrent.PromiseListener;

public interface ChannelPromise extends Promise<Void> {

  /** Return the {@link Channel} it related with */
  Channel channel();

  @Override
  ChannelPromise addListener(PromiseListener<? extends Promise<? super Void>> listener);

  @Override
  ChannelPromise await() throws InterruptedException;
}
