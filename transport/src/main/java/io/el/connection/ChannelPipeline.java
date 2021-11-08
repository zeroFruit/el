package io.el.connection;

public interface ChannelPipeline extends ChannelInboundInvoker {
    ChannelPipeline addLast(ChannelHandler... handler);

    @Override
    ChannelPipeline fireChannelRegistered();

    @Override
    ChannelPipeline fireChannelRead(Object msg);
}
