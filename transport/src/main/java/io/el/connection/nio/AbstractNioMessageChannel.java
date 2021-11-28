package io.el.connection.nio;

import io.el.connection.Channel;
import io.el.connection.ChannelOutboundBuffer;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.List;

/**
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on messages.
 */
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

  protected AbstractNioMessageChannel(SelectableChannel ch, int readInterestOp) {
    super(ch, readInterestOp);
  }

  @Override
  protected AbstractNioInternal newInternal() {
    return new NioMessageInternal();
  }

  @Override
  protected void doBeginRead() throws Exception {
    super.doBeginRead();
  }

  @Override
  protected void doWrite(ChannelOutboundBuffer in) throws Exception {
    // TODO: implement me
  }

  private final class NioMessageInternal extends AbstractNioInternal {

    @Override
    public void read() {
      // TODO: implement me
    }
  }

  /**
   * Read messages into the given array and return the amount which was read.
   */
  protected abstract int doReadMessages(List<Object> buf) throws Exception;

  /**
   * Write a message to the underlying {@link java.nio.channels.Channel}.
   *
   * @return {@code true} if and only if the message has been written
   */
  protected abstract boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception;
}
