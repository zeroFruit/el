package io.el.channel.local;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.el.channel.ChannelId;
import io.el.channel.ChannelPromise;
import io.el.channel.DefaultChannelEventLoop;
import io.el.channel.DefaultChannelEventLoopGroup;
import io.el.channel.DefaultChannelPromise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LocalChannelTests {
  private DefaultChannelEventLoopGroup group;
  private DefaultChannelEventLoop eventLoop;

  @BeforeEach
  public void setUp() {
    group = new DefaultChannelEventLoopGroup(1);
    eventLoop = new DefaultChannelEventLoop(group);
  }

  @Nested
  @DisplayName("On register() method")
  class RegisterMethod {
    @Test
    @DisplayName("when register, then it registered to eventLoop")
    public void register() throws InterruptedException {

      LocalServerChannel serverChannel = new LocalServerChannel(ChannelId.of("server1"));
      LocalChannel clientChannel = new LocalChannel(ChannelId.of("client1"));

      // both channel need to be registered to eventLoop
      ChannelPromise promise0 = new DefaultChannelPromise(serverChannel, eventLoop);
      ChannelPromise promise1 = new DefaultChannelPromise(clientChannel, eventLoop);
      serverChannel.internal().register(eventLoop, promise0);
      clientChannel.internal().register(eventLoop, promise1);

      promise0.await();
      promise1.await();

      assertTrue(promise0.isSuccess());
      assertTrue(promise1.isSuccess());

      assertNotNull(clientChannel.channelEventLoop());
      assertNotNull(serverChannel.channelEventLoop());

      // both channel need to be in the inactive state because they're not connected each other
      assertFalse(clientChannel.isActive());
      assertFalse(serverChannel.isActive());
    }
  }
}
