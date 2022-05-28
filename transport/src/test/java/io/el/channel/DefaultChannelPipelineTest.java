package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.el.concurrent.ThreadPerTaskExecutor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DefaultChannelPipelineTest {

  @Nested
  @DisplayName("On firstContext() method")
  class FirstContextMethod {

    @Test
    @DisplayName("When there is no handler, then returns null")
    public void returnsNullOnEmpty() {
      Channel channel = mock(Channel.class);
      final DefaultChannelPipeline channelPipeline = new DefaultChannelPipeline(channel);
      assertNull(channelPipeline.firstContext());
    }

    @Test
    @DisplayName("When there is a handler, then returns it")
    public void returnsTheOnlyOne() {
      Channel channel = mock(Channel.class);
      final DefaultChannelPipeline channelPipeline = new DefaultChannelPipeline(channel);

      ChannelHandler handler = mock(ChannelHandler.class);
      channelPipeline.addLast(handler);
      assertEquals(channelPipeline.firstContext().handler(), handler);
    }

    @Test
    @DisplayName("When there are multiple handlers, then returns the first one")
    public void returnsTheFirst() {
      Channel channel = mock(Channel.class);
      final DefaultChannelPipeline channelPipeline = new DefaultChannelPipeline(channel);

      ChannelHandler handler = mock(ChannelHandler.class);
      channelPipeline.addLast(handler);

      for (int i = 0; i < 10; i = i + 1) {
        ChannelHandler nextHandler = mock(ChannelHandler.class);
        channelPipeline.addLast(nextHandler);
      }

      assertEquals(channelPipeline.firstContext().handler(), handler);
    }
  }

  @Nested
  @DisplayName("On remove() method")
  class RemoveMethod {

    @Test
    @DisplayName("When remove all handler, then the first is null")
    public void removeOnlyOne() {
      Channel channel = mock(Channel.class);
      final DefaultChannelPipeline channelPipeline = new DefaultChannelPipeline(channel);

      ChannelHandler handler = mock(ChannelHandler.class);

      channelPipeline.addLast(handler);
      assertEquals(channelPipeline.firstContext().handler(), handler);

      channelPipeline.remove(handler);
      assertNull(channelPipeline.firstContext());
    }
  }

  @Nested
  @DisplayName("On bind() method")
  class BindMethod {
    @Test
    public void checkChannelBindCalled() throws InterruptedException {
      Executor executor = new ThreadPerTaskExecutor(Executors.defaultThreadFactory());
      final DefaultChannelEventLoop eventLoop = new DefaultChannelEventLoop(executor, null);

      try {
        final CountDownLatch latch = new CountDownLatch(1);
        Channel channel =
            new TestChannel() {
              @Override
              public ChannelPromise bind(SocketAddress localAddress) {
                assertEquals(((InetSocketAddress) localAddress).getPort(), 8080);
                latch.countDown();
                return null;
              }
            };
        channel.register(eventLoop);

        DefaultChannelPipeline channelPipeline = new DefaultChannelPipeline(channel);
        channelPipeline.bind(new InetSocketAddress(8080));

        assertTrue(latch.await(1L, TimeUnit.SECONDS));
      } finally {
        eventLoop.shutdownGracefully(1, TimeUnit.SECONDS);
      }
    }
  }

  private static class TestChannel extends AbstractChannel {
    private boolean active;
    private boolean closed;
    private boolean registered;

    protected TestChannel() {
      super(null);
    }

    @Override
    public boolean isOpen() {
      return !closed;
    }

    @Override
    public boolean isRegistered() {
      return registered;
    }

    @Override
    public boolean isActive() {
      return isOpen() && active;
    }

    @Override
    protected AbstractInternal newInternal() {
      return new MyInternal();
    }

    @Override
    protected void register() {

    }

    private class MyInternal extends AbstractInternal {

      @Override
      public SocketAddress localAddress() {
        return null;
      }

      @Override
      public SocketAddress remoteAddress() {
        return null;
      }
    }
  }
}
