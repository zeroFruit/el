package io.el.connection.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * An arbitrary task that can be executed by {@link NioChannelEventLoop} when a {@link SelectableChannel} becomes ready.
 */
public interface NioTask<C extends SelectableChannel> {
  /**
   * Invoked when the {@link SelectableChannel} has been selected by the {@link Selector}.
   */
  void channelReady(C ch, SelectionKey key) throws Exception;
}
