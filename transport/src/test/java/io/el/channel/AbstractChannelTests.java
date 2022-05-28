package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractChannelTests {
  @Nested
  @DisplayName("On register() method")
  class RegisterMethod {

    @Test
    @DisplayName("when in event loop, then fire channel register handlers")
    public void fireChannelRegisterHandlers() throws Exception {
      ChannelEventLoop eventLoop = mock(ChannelEventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);
      CountDownLatch latch = new CountDownLatch(1);
      TestInboundHandler handler =
          new TestInboundHandler() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
              latch.countDown();
            }
          };

      TestChannel channel = new TestChannel();
      channel.pipeline().addLast(handler);

      ChannelPromise promise = new DefaultChannelPromise(channel, eventLoop);
      channel.internal().register(eventLoop, promise);
      promise.await();
      latch.await();

      assertTrue(promise.isSuccess());
    }

    @Test
    @DisplayName("when an exception is thrown in the handler, then exceptionCaught is called")
    public void fireExceptionInRegistered() throws Exception {
      ChannelEventLoop eventLoop = mock(ChannelEventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);
      CountDownLatch latch = new CountDownLatch(1);

      class TestException extends RuntimeException {}

      TestInboundHandler handler =
          new TestInboundHandler() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) {
              throw new TestException();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
              if (cause instanceof TestException) {
                latch.countDown();
              } else {
                Assertions.fail();
              }
            }
          };

      TestChannel channel = new TestChannel();
      channel.pipeline().addLast(handler);

      ChannelPromise promise = new DefaultChannelPromise(channel, eventLoop);
      channel.internal().register(eventLoop, promise);
      promise.await();

      boolean latchFired = latch.await(1, TimeUnit.SECONDS);
      assertTrue(latchFired);
    }
  }

  @Test
  @DisplayName("when in event loop, then fire connect handlers")
  public void connect() throws Exception {
    ChannelEventLoop eventLoop = mock(ChannelEventLoop.class);
    when(eventLoop.inEventLoop()).thenReturn(true);
    CountDownLatch latch = new CountDownLatch(1);
    TestOutboundHandler handler =
        new TestOutboundHandler() {
          @Override
          public void connect(
              ChannelHandlerContext ctx, SocketAddress remoteAddress, ChannelPromise promise)
              throws Exception {
            latch.countDown();
            ctx.connect(remoteAddress, promise);
          }
        };

    TestChannel channel =
        new TestChannel() {
          @Override
          protected AbstractInternal newInternal() {
            return new TestChannel.TestAbstractInternal() {

              @Override
              public void connect(SocketAddress remoteAddress, ChannelPromise promise) {
                promise.setSuccess(null);
              }
            };
          }
        };
    channel.pipeline().addLast(handler);

    ChannelPromise registerPromise = new DefaultChannelPromise(channel, eventLoop);
    channel.internal().register(eventLoop, registerPromise);
    registerPromise.await();

    InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);
    ChannelPromise promise = channel.connect(remoteAddress);
    promise.await();
    boolean latchResult = latch.await(1, TimeUnit.SECONDS);
    assertTrue(latchResult);

    assertTrue(promise.isSuccess());
  }

  @Nested
  @DisplayName("On bind() method")
  class BindMethod {

    @Test
    @DisplayName("when in event loop, then bind local address")
    public void bindLocalAddress() throws Exception {
      ChannelEventLoop eventLoop = mock(ChannelEventLoop.class);
      when(eventLoop.inEventLoop()).thenReturn(true);
      CountDownLatch latch = new CountDownLatch(1);
      TestInboundHandler handler =
          new TestInboundHandler() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
              latch.countDown();
            }
          };

      TestChannel channel = new TestChannel();
      channel.pipeline().addLast(handler);

      ChannelPromise eventLoopPromise = new DefaultChannelPromise(channel, eventLoop);
      channel.internal().register(eventLoop, eventLoopPromise);
      eventLoopPromise.await();
      latch.await();
      assertTrue(eventLoopPromise.isSuccess());

      SocketAddress localAddress = InetSocketAddress.createUnresolved("localhost", 8080);
      ChannelPromise bindPromise = channel.bind(localAddress);
      bindPromise.await();
      assertEquals(localAddress, channel.localAddress);
      assertTrue(bindPromise.isSuccess());
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

  private abstract class TestOutboundHandler implements ChannelOutboundHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      fail("should not be called");
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
        throws Exception {
      fail("should not be called");
    }

    @Override
    public void connect(
        ChannelHandlerContext ctx, SocketAddress remoteAddress, ChannelPromise promise)
        throws Exception {
      fail("should not be called");
    }
  }

  class TestChannel extends AbstractChannel {

    private SocketAddress localAddress;

    protected TestChannel() {
      this(ChannelId.of("TEST_CHANNEL"));
    }

    protected TestChannel(ChannelId id) {
      super(id);
    }

    public void setLocalAddress(SocketAddress localAddress) {
      this.localAddress = localAddress;
    }

    @Override
    protected AbstractInternal newInternal() {
      return new TestAbstractInternal();
    }

    public class TestAbstractInternal extends AbstractInternal {

      @Override
      public SocketAddress localAddress() {
        return null;
      }

      @Override
      public SocketAddress remoteAddress() {
        return null;
      }

      @Override
      public void connect(SocketAddress remoteAddress, ChannelPromise promise) {}

      @Override
      public void doBind(SocketAddress localAddress) {
        setLocalAddress(localAddress);
      }
    }

    @Override
    protected void doRegister() {
      // NO-OP
    }

    @Override
    public boolean isOpen() {
      return false;
    }

    @Override
    public boolean isActive() {
      return false;
    }
  }
}
