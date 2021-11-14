package io.el.connection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class AbstractChannelTests {
  @Test
  public void ensureInitialRegistrationFiresActive() throws Throwable {
    ChannelEventLoop eventLoop = mock(ChannelEventLoop.class);

    // This allows us to have a single-threaded test
    when(eventLoop.inEventLoop()).thenReturn(true);

    TestChannel channel = new TestChannel();
    ChannelInboundHandler handler = mock(ChannelInboundHandler.class);
    channel.pipeline().addLast(handler);

    assertTrue(registerChannel(eventLoop, channel).isSuccess());

    verify(handler).handlerAdded(any(ChannelHandlerContext.class));
    verify(handler).channelRegistered(any(ChannelHandlerContext.class));
  }

  private static ChannelPromise registerChannel(ChannelEventLoop loop, Channel ch)
      throws Exception {
    DefaultChannelPromise promise = new DefaultChannelPromise(ch);
    ch.register(loop, promise);
    promise.await();
    return promise;
  }

  private static class TestChannel extends AbstractChannel {

    @Override
    public boolean isOpen() {
      return true;
    }
  }
}
