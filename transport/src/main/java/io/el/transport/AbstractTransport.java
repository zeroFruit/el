package io.el.transport;

import io.el.channel.Channel;
import io.el.channel.ChannelEventLoopGroup;
import io.el.channel.ChannelFactory;
import io.el.channel.ReflectiveChannelFactory;
import io.el.internal.ObjectUtil;

public abstract class AbstractTransport<T extends AbstractTransport<T, C>, C extends Channel> {
  private volatile ChannelEventLoopGroup acceptorGroup;

  private volatile ChannelFactory<C> channelFactory;

  public T group(ChannelEventLoopGroup acceptorGroup) {
    ObjectUtil.checkNotNull(acceptorGroup, "group");
    if (this.acceptorGroup != null) {
      throw new IllegalStateException("group set already");
    }
    this.acceptorGroup = acceptorGroup;
    return self();
  }

  public T channel(Class<? extends C> channelClass) {
    this.channelFactory = new ReflectiveChannelFactory<C>(
        ObjectUtil.checkNotNull(channelClass, "channelClass")
    );
    return self();
  }

  private T self() {
    return (T) this;
  }
}
