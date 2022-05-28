package io.el.channel;

import io.el.concurrent.AbstractEventLoopGroup;
import io.el.concurrent.EventLoop;
import io.el.concurrent.EventLoopChooserFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DefaultChannelEventLoopGroup extends AbstractEventLoopGroup
    implements ChannelEventLoopGroup {

  public DefaultChannelEventLoopGroup(int nThreads) {
    super(nThreads, null, null);
  }

  /**
   * * Create {@link ChannelEventLoop} children with size of {@param maxChannels}, then add chooser
   * with {@param chooserFactory}
   *
   * @param nThreads
   * @param executor
   * @param chooserFactory
   */
  public DefaultChannelEventLoopGroup(
      int nThreads, Executor executor, EventLoopChooserFactory chooserFactory) {
    // TODO: change after chooser factory implemented
    super(nThreads, executor, chooserFactory);
  }

  @Override
  protected EventLoop newChild(Executor executor) {
    return new DefaultChannelEventLoop(executor, this);
  }

  @Override
  protected ThreadFactory newDefaultThreadFactory() {
    return new DefaultThreadFactory();
  }

  @Override
  public ChannelEventLoop next() {
    return (ChannelEventLoop) super.next();
  }

  @Override
  public ChannelPromise register(Channel channel) {
    return next().register(channel);
  }

  @Override
  public boolean shutdownGracefully(long timeout, TimeUnit unit) {
    return super.shutdownGracefully(timeout, unit);
  }

  @Override
  public boolean isShuttingDown() {
    return super.isShuttingDown();
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return super.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return super.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    super.execute(command);
  }
}
