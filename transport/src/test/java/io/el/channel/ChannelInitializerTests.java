package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.el.channel.local.LocalChannel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ChannelInitializerTests {
  private static final int TIMEOUT_MILLIS = 1000;
  private ChannelEventLoopGroup group;

  @BeforeEach
  public void setUp() {
    group = new DefaultChannelEventLoopGroup(1);
  }

  @AfterEach
  public void tearDown() {
    group.shutdownGracefully(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
  }

  @Nested
  @DisplayName("On initChannel() method")
  class HandlerAddedMethod {
    @Test
    @DisplayName("When after registered to event loop, then initializer should be removed")
    public void initializerShouldBeRemoved() {
      ChannelPipeline pipeline = new LocalChannel(ChannelId.of("ch1")).pipeline();
      ChannelHandler handler = new TestInboundHandler();

      pipeline.addLast(
          new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
              ch.pipeline().addLast(handler);
            }
          });
      try {
        group.register(pipeline.channel()).await();
      } catch (InterruptedException e) {
        fail("exception thrown", e);
      }

      Iterator<Entry<String, ChannelHandler>> iter = pipeline.iterator();
      assertTrue(iter.hasNext());
      assertEquals(handler, iter.next().getValue());
      assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("When exception is thrown, then initializer should catch the exception")
    public void exceptionThrown() {
      final Exception exception = new Exception();
      final AtomicReference<Throwable> causeRef = new AtomicReference<>();
      ChannelPipeline pipeline = new LocalChannel(ChannelId.of("ch1")).pipeline();

      pipeline.addLast(
          new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
              throw exception;
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
              causeRef.set(cause);
            }
          });

      try {
        group.register(pipeline.channel()).await();
      } catch (InterruptedException e) {
        fail("exception thrown", e);
      }
      assertEquals(exception, causeRef.get());
    }
  }

  private class TestInboundHandler implements ChannelInboundHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      // NO-OP
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      // NO-OP
    }
  }
}
