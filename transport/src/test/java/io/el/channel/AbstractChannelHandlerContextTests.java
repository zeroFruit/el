package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.el.concurrent.EventLoop;
import java.net.SocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractChannelHandlerContextTests {
  @Nested
  @DisplayName("On fireExceptionCaught() method")
  class FireExceptionCaughtMethod {

    @Test
    @DisplayName("when next context not exist, then throw null exception")
    public void testNextNotExist() {
      assertThrows(
          NullPointerException.class,
          () -> {
            ChannelPipeline pipeline = mock(ChannelPipeline.class);
            EventLoop eventLoop = mock(EventLoop.class);
            when(eventLoop.inEventLoop()).thenReturn(true);

            AbstractChannelHandlerContext curr =
                new TestChannelHandlerContext(
                    "curr", pipeline, eventLoop, new TestInboundHandler() {});
            // next context not exist

            curr.fireExceptionCaught(new TestException("test"));
          });
    }

    @Test
    @DisplayName("when in event loop, then invoke handler")
    public void testInEventLoop() {
      ChannelPipeline pipeline = mock(ChannelPipeline.class);
      EventLoop eventLoop = mock(EventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);

      AbstractChannelHandlerContext curr =
          new TestChannelHandlerContext("curr", pipeline, eventLoop, new TestInboundHandler() {});
      curr.next =
          new TestChannelHandlerContext(
              "next",
              pipeline,
              eventLoop,
              new TestInboundHandler() {
                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                  assertEquals(ctx, curr.next);
                  assertEquals(cause.getMessage(), "test");
                }
              });

      curr.fireExceptionCaught(new TestException("test"));
    }
  }

  @Nested
  @DisplayName("On fireChannelRegistered() method")
  class FireChannelRegisteredMethod {
    @Test
    @DisplayName("when in event loop, then invoke handler exception handler")
    public void testInEventLoop() {
      ChannelPipeline pipeline = mock(ChannelPipeline.class);
      EventLoop eventLoop = mock(EventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);

      AbstractChannelHandlerContext curr =
          new TestChannelHandlerContext("curr", pipeline, eventLoop, new TestInboundHandler() {});
      curr.next =
          new TestChannelHandlerContext(
              "next",
              pipeline,
              eventLoop,
              new TestInboundHandler() {
                @Override
                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                  assertEquals(ctx, curr.next);
                }
              });

      curr.fireChannelRegistered();
    }

    @Test
    @DisplayName("when throw exception, then catch on handler")
    public void testException() {
      ChannelPipeline pipeline = mock(ChannelPipeline.class);
      EventLoop eventLoop = mock(EventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);

      AbstractChannelHandlerContext curr =
          new TestChannelHandlerContext("curr", pipeline, eventLoop, new TestInboundHandler() {});
      curr.next =
          new TestChannelHandlerContext(
              "next",
              pipeline,
              eventLoop,
              new TestInboundHandler() {
                @Override
                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                  throw new TestException("test");
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                  assertEquals(ctx, curr.next);
                  assertEquals(cause.getMessage(), "test");
                }
              });

      curr.fireChannelRegistered();
    }
  }

  private static class TestException extends RuntimeException {
    TestException() {}

    TestException(String message) {
      super(message);
    }
  }

  private abstract class TestInboundHandler implements ChannelInboundHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      fail("should not be called");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      fail("should not be called");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      fail("should not be called");
    }
  }

  private static class TestChannelHandlerContext extends AbstractChannelHandlerContext {
    private final ChannelHandler handler;

    TestChannelHandlerContext(
        String name, ChannelPipeline pipeline, EventLoop eventLoop, ChannelHandler handler) {
      super(name, pipeline, eventLoop, handler.getClass());
      this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
      return handler;
    }

    @Override
    public ChannelPromise bind(SocketAddress localAddress) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelPromise connect(
        SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      throw new UnsupportedOperationException();
    }
  }
}
