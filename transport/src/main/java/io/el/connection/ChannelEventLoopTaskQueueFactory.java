package io.el.connection;

import java.util.Queue;

/**
 * Factory used to create {@link Queue} instances that will be used to store tasks for an {@link ChannelEventLoop}.
 *
 * Generally speaking the returned {@link Queue} MUST be thread-safe and depending on the {@link ChannelEventLoop}
 * implementation must be of type {@link java.util.concurrent.BlockingQueue}.
 */
public interface ChannelEventLoopTaskQueueFactory {
  Queue<Runnable> newTaskQueue(int maxCapacity);
}
