package io.el.channel;

import java.util.UUID;

public final class ChannelId {
  private final String value;

  private ChannelId(String value) {
    this.value = value;
  }

  public static ChannelId of(String value) {
    return new ChannelId(value);
  }

  public static ChannelId generate() {
    // TODO: generate channel id with the process related data, such as process id
    String randStr = UUID.randomUUID().toString().substring(0, 8);
    return ChannelId.of(randStr);
  }

  @Override
  public String toString() {
    return value;
  }
}
