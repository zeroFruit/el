package io.el.connection;

public interface ChannelInboundInvoker {
    ChannelInboundInvoker fireChannelRegistered();

    ChannelInboundInvoker fireChannelRead(Object msg);
}
