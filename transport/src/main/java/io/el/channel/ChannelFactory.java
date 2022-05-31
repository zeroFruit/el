package io.el.channel;

/**
 * Create a new {@link Channel}.
 * */
public interface ChannelFactory<T extends Channel> {
  /**
   * Creates a new channel.
   */
  T newChannel();
}
