package io.el.connection.socket;

import io.el.connection.Channel;
import java.net.InetSocketAddress;

public interface ServerSocketChannel extends Channel {
  @Override
  InetSocketAddress localAddress();
  @Override
  InetSocketAddress remoteAddress();
}
