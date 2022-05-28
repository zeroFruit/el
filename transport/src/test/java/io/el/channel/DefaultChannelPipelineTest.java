package io.el.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

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
}
