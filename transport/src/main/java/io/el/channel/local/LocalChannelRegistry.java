package io.el.channel.local;

import io.el.channel.Channel;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalChannelRegistry {

  private static final ConcurrentMap<LocalAddress, Channel> boundChannels = new ConcurrentHashMap<>();

  public static LocalAddress register(Channel channel, SocketAddress localAddress) {
    if (!(localAddress instanceof LocalAddress)) {
      throw new IllegalStateException("unsupported address type: " + LocalAddress.class);
    }

    LocalAddress address = (LocalAddress) localAddress;

    Channel boundChannel = boundChannels.putIfAbsent(address, channel);
    if (boundChannel != null) {
      throw new IllegalStateException("address already in use by: " + boundChannel);
    }
    return address;
  }
}
