package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
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

  class TestChannel extends AbstractChannel {

    protected TestChannel() {
      this(ChannelId.of("TEST_CHANNEL"));
    }

    protected TestChannel(ChannelId id) {
      super(id);
    }

    @Override
    protected AbstractInternal newInternal() {
      return new AbstractInternal() {
        @Override
        public SocketAddress localAddress() {
          return null;
        }

        @Override
        public SocketAddress remoteAddress() {
          return null;
        }
      };
    }

    @Override
    protected void register() {
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
