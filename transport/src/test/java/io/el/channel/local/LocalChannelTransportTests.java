package io.el.channel.local;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.el.channel.Channel;
import io.el.channel.ChannelHandlerContext;
import io.el.channel.ChannelInboundHandler;
import io.el.channel.ChannelInitializer;
import io.el.channel.DefaultChannelEventLoopGroup;
import io.el.transport.ClientTransport;
import io.el.transport.ServerTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link LocalChannel} and {@link LocalServerChannel} with the use of {@link
 * ClientTransport} and {@link ServerTransport}
 */
public class LocalChannelTransportTests {
  private static final String SERVER_ADDRESS = "server:addr";

  private DefaultChannelEventLoopGroup group1, group2;

  @BeforeEach
  public void setUp() {
    group1 = new DefaultChannelEventLoopGroup(1);
    group2 = new DefaultChannelEventLoopGroup(1);
  }

  @Test
  @DisplayName("When client connect to server, both server and client is in active state")
  public void localChannel() throws InterruptedException {
    ServerTransport st = new ServerTransport();
    ClientTransport ct = new ClientTransport();

    st.group(group1)
        .channel(LocalServerChannel.class)
        .handler(
            new ChannelInitializer<LocalChannel>() {
              @Override
              protected void initChannel(LocalChannel ch) throws Exception {
                ch.pipeline().addLast(new TestHandler());
              }
            });
    ct.group(group2).channel(LocalChannel.class).handler(new TestHandler());

    Channel sc = st.bind(new LocalAddress(SERVER_ADDRESS)).await().channel();
    Channel cc = ct.connect(sc.localAddress()).await().channel();

    assertTrue(sc.isActive());
    assertTrue(cc.isActive());
  }

  private static class TestHandler implements ChannelInboundHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      ctx.fireChannelRegistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.fireExceptionCaught(cause);
    }
  }
}
