package io.el.channel.local;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.el.channel.ChannelId;
import io.el.channel.ChannelPromise;
import io.el.channel.DefaultChannelEventLoop;
import io.el.channel.DefaultChannelEventLoopGroup;
import io.el.channel.DefaultChannelPromise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LocalChannelTests {
  @Nested
  @DisplayName("On register() method")
  class RegisterMethod {
    @Test
    @DisplayName("when register, then it registered to eventLoop")
    public void register() throws InterruptedException {
      DefaultChannelEventLoopGroup group = new DefaultChannelEventLoopGroup(1);
      DefaultChannelEventLoop eventLoop = new DefaultChannelEventLoop(group);
      LocalServerChannel serverChannel = new LocalServerChannel(ChannelId.of("server1"));
      LocalChannel clientChannel1 = new LocalChannel(ChannelId.of("client1"));

      // server channel creates `client2` which talks to `client1`
      LocalChannel clientChannel2 =
          serverChannel.newLocalChannel(ChannelId.of("client2"), clientChannel1);

      // both channel need to be registered to eventLoop
      ChannelPromise promise1 = new DefaultChannelPromise(clientChannel1, eventLoop);
      ChannelPromise promise2 = new DefaultChannelPromise(clientChannel2, eventLoop);
      clientChannel1.internal().register(eventLoop, promise1);
      clientChannel2.internal().register(eventLoop, promise2);

      promise1.await();
      promise2.await();

      assertTrue(promise1.isSuccess());
      assertTrue(promise2.isSuccess());
      assertNotNull(clientChannel1.channelEventLoop());
      assertNotNull(clientChannel2.channelEventLoop());
      // both channel need to be in the active state
      assertTrue(clientChannel1.isActive());
      assertTrue(clientChannel2.isActive());
    }
  }
}
