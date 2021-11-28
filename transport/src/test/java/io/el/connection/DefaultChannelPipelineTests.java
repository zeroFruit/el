package io.el.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.el.concurrent.EventLoopGroup;
import io.el.connection.local.LocalChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class DefaultChannelPipelineTests {
  @Test
  public void testChannelHandlerContextNavigation() {
    ChannelPipeline pipeline = new LocalChannel().pipeline();

    final int HANDLER_ARRAY_LEN = 5;
    ChannelHandler[] lastHandlers = newHandlers(HANDLER_ARRAY_LEN);

    pipeline.addLast(lastHandlers);

    verifyContextNubmer(pipeline, HANDLER_ARRAY_LEN);
  }

  @Test
  public void testFireChannelRegistered() throws Exception {
    ChannelEventLoopGroup group = new DefaultChannelEventLoopGroup(1);
    final CountDownLatch latch = new CountDownLatch(1);

    ChannelPipeline pipeline = new LocalChannel().pipeline();

    pipeline.addLast(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
          @Override
          public void channelRegistered(ChannelHandlerContext ctx) {
            latch.countDown();
          }
        });
      }
    });
    group.register(pipeline.channel());
    assertTrue(latch.await(2, TimeUnit.SECONDS));
  }

  private static void verifyContextNubmer(ChannelPipeline pipeline, int expectedNumber) {
    AbstractChannelHandlerContext ctx = (AbstractChannelHandlerContext) pipeline.firstContext();
    int handlerNumber = 0;
    while (ctx != ((DefaultChannelPipeline) pipeline).tail) {
      handlerNumber += 1;
      ctx = ctx.next;
    }
    assertEquals(expectedNumber, handlerNumber);
  }

  private static ChannelHandler[] newHandlers(int num) {
    assert num > 0;

    ChannelHandler[] handlers = new ChannelHandler[num];
    for (int i = 0; i < num; i++) {
      handlers[i] = newHandler();
    }
    return handlers;
  }

  private static ChannelHandler newHandler() {
    return new TestHandler();
  }

  private static class TestHandler extends ChannelDuplexHandler {}
}
