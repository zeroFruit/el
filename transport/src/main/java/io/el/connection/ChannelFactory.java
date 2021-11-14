package io.el.connection;

public interface ChannelFactory<C extends Channel> {

  C newChannel();
}
