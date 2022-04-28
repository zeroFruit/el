package io.el.channel;

public final class ChannelId {
  private final String value;

  private ChannelId(String value) {
    this.value = value;
  }

  public static ChannelId of(String value) {
    return new ChannelId(value);
  }
}
