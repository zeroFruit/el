package io.el.connection.nio;

import io.el.connection.Channel;
import io.el.connection.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;

public class NioEventLoopTests {
  @Test
  public void testSelectableChannel() throws Exception {

  }

  @Test
  public void testChannelsRegistered() throws Exception {
    NioChannelEventLoopGroup group = new NioChannelEventLoopGroup(1);
    NioChannelEventLoop loop = (NioChannelEventLoop) group.next();

    try {
      Channel channel = new NioServerSocketChannel();
    } finally {

    }
  }
}
