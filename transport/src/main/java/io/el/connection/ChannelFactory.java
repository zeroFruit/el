package io.el.connection;

public interface ChannelFactory<C extends ChannelFactory> {
    C newChannel();
}
