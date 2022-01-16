package io.el.connection.nio;

import io.el.connection.Channel;
import io.el.connection.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class NioEventLoopTests {
  @Test
  public void testSelectableChannel() throws Exception {
    NioChannelEventLoopGroup group = new NioChannelEventLoopGroup(1);
    NioChannelEventLoop loop = (NioChannelEventLoop) group.next();

    try {
      Channel channel = new NioServerSocketChannel();
      loop.register(channel).await();
      channel.bind(new InetSocketAddress(0)).await();

//      SocketChannel selectableChannel = SocketChannel.open();
//      selectableChannel.configureBlocking(false);
//      selectableChannel.connect(channel.localAddress());
//
//      final CountDownLatch latch = new CountDownLatch(1);
//
//      loop.register(selectableChannel, SelectionKey.OP_CONNECT, new NioTask<SocketChannel>() {
//        @Override
//        public void channelReady(SocketChannel ch, SelectionKey key) throws Exception {
//          latch.countDown();
//        }
//      });
//
//      latch.await();
//      selectableChannel.close();
    } finally {
      group.shutdownGracefully(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void testChannelsRegistered() throws Exception {

  }
}
