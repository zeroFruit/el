package io.el.connection.local;

import io.el.bootstrap.ServerBootstrap;
import io.el.concurrent.EventLoopGroup;
import io.el.connection.Channel;
import io.el.connection.ChannelEventLoopGroup;
import io.el.connection.ChannelHandlerContext;
import io.el.connection.ChannelInboundHandlerAdapter;
import io.el.connection.ChannelInitializer;
import io.el.connection.DefaultChannelEventLoopGroup;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalChannelTest {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final LocalAddress TEST_ADDRESS = new LocalAddress("test.id");

  private static ChannelEventLoopGroup group1;
  private static ChannelEventLoopGroup group2;
  private static EventLoopGroup sharedGroup;

  @BeforeAll
  public static void beforeClass() {
    group1 = new DefaultChannelEventLoopGroup(2);
    group2 = new DefaultChannelEventLoopGroup(2);
    sharedGroup = new DefaultChannelEventLoopGroup(2);
  }

  @Test
  public void testLocalAddressReuse() {
    for (int i = 0; i < 2; i++) {
      ServerBootstrap sb = new ServerBootstrap();
      sb
          .group(group1, group2)
          .childHandler(new ChannelInitializer<LocalChannel>() {
            @Override
            protected void initChannel(LocalChannel ch) throws Exception {
              ch.pipeline().addLast(new TestHandler());
            }
          });

      Channel sc = null;
      Channel cc = null;

      try {
        sc = sb.bind(TEST_ADDRESS).await().channel();

        final CountDownLatch latch = new CountDownLatch(1);
        // Connect to the server
//        cc =
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  static class TestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      LOGGER.info(String.format("Received message: %s", msg));
    }
  }
}
