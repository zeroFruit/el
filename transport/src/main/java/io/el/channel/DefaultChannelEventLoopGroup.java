package io.el.channel;

import io.el.concurrent.DefaultEventLoopGroup;
import io.el.concurrent.EventLoop;
import io.el.concurrent.EventLoopChooserFactory;
import io.el.concurrent.Promise;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultChannelEventLoopGroup extends DefaultEventLoopGroup implements
    ChannelEventLoopGroup {

  /***
   * Create {@link ChannelEventLoop} children with size of {@param maxChannels},
   * then add chooser with {@param chooserFactory}
   * @param maxChannels
   * @param executor
   * @param chooserFactory
   */
  public DefaultChannelEventLoopGroup(
      int maxChannels,
      Executor executor,
      EventLoopChooserFactory chooserFactory
  ) {
    // maybe, change after chooser factory implemented
    super(maxChannels, executor, chooserFactory);
  }

  // TODO: implements this after ChannelEventLoop implemented
  @Override
  protected EventLoop newChild(Executor executor) throws Exception {
    return null;
  }

  // TODO: implements this after ThreadFactory implemented
  @Override
  protected ThreadFactory newDefaultThreadFactory() {
    return null;
  }

  @Override
  public ChannelEventLoop next() {
    return (ChannelEventLoop) super.next();
  }

  @Override
  public ChannelPromise register(Channel channel) {
    return channel.register(this.next());
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

  @Override
  public <V> Promise<V> submit(Callable<V> task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Promise<?> submit(Runnable task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<EventLoop> iterator() {
    return super.iterator();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    throw new UnsupportedOperationException();
  }
}
