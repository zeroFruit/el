package io.el.transport;

import io.el.channel.ChannelEventLoopGroup;

public class ServerTransport extends AbstractTransport {
  private volatile ChannelEventLoopGroup clientGroup;

  public ServerTransport group(ChannelEventLoopGroup acceptorGroup, ChannelEventLoopGroup clientGroup) {
    super.group(acceptorGroup);
    if (this.clientGroup != null) {
      throw new IllegalStateException("childGroup set already");
    }
    this.clientGroup = clientGroup;
    return this;
  }

  public ServerTransport handler() {

  }
}
