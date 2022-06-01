package io.el.transport;

import io.el.channel.Channel;
import io.el.channel.ChannelEventLoopGroup;
import io.el.channel.ChannelHandler;
import io.el.channel.ChannelInitializer;
import io.el.channel.ChannelPipeline;
import io.el.internal.ObjectUtil;

public class ServerTransport extends AbstractTransport<ServerTransport, Channel> {
  private volatile ChannelEventLoopGroup clientGroup;
  private volatile ChannelHandler channelHandler;

  public ServerTransport group(
      ChannelEventLoopGroup parentGroup, ChannelEventLoopGroup clientGroup) {
    super.group(parentGroup);
    if (this.clientGroup != null) {
      throw new IllegalStateException("childGroup set already");
    }
    this.clientGroup = clientGroup;
    return this;
  }

  public ServerTransport handler(ChannelHandler channelHandler) {
    this.channelHandler = ObjectUtil.checkNotNull(channelHandler, "childHandler");
    return this;
  }

  @Override
  void init(Channel channel) throws Exception {
    ChannelPipeline pipeline = channel.pipeline();

    pipeline.addLast(
        new ChannelInitializer() {
          @Override
          protected void initChannel(Channel chan) throws Exception {
            chan.channelEventLoop()
                .execute(
                    () -> {
                      pipeline.addLast(channelHandler);
                    });
          }
        });
  }
}
