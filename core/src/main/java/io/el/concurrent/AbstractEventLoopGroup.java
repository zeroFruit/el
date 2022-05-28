package io.el.concurrent;

import static io.el.internal.ObjectUtil.checkPositive;

import io.el.concurrent.EventLoopChooserFactory.EventLoopChooser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventLoopGroup implements EventLoopGroup {

  static final long DEFAULT_TIMEOUT = 15;
  private final List<EventLoop> children;
  private final EventLoopChooser chooser;

  /**
   * * Create {@link EventLoop} children with size of {@param nThreads}, then add chooser with
   * {@param chooserFactory}
   */
  protected AbstractEventLoopGroup(
      int nThreads, Executor executor, EventLoopChooserFactory chooserFactory) {
    checkPositive(nThreads, "nThreads");

    if (executor == null) {
      executor = new ThreadPerTaskExecutor(this.newDefaultThreadFactory());
    }

    this.children = new ArrayList<>();
    for (int i = 0; i < nThreads; i++) {
      try {
        this.children.add(this.newChild(executor));
      } catch (Exception e) {
        for (int j = 0; j < i; j++) {
          this.children.get(j).shutdownGracefully(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        }
        this.children.forEach(
            child -> {
              try {
                while (!child.isTerminated()) {
                  child.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
                }
              } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
              }
            });
        throw new IllegalStateException("failed to create a child event loop", e);
      }
    }

    this.chooser = chooserFactory.newChooser(this.children);
  }

  protected abstract EventLoop newChild(Executor executor) throws Exception;

  protected abstract ThreadFactory newDefaultThreadFactory();

  @Override
  public EventLoop next() {
    return this.chooser.next();
  }

  @Override
  public Iterator<EventLoop> iterator() {
    return this.children.iterator();
  }

  @Override
  public boolean shutdownGracefully(long timeout, TimeUnit unit) {
    return this.children.stream()
        .map(c -> c.shutdownGracefully(timeout, unit))
        .filter(s -> s.equals(false))
        .findAny()
        .orElse(true);
  }

  @Override
  public boolean isShuttingDown() {
    return this.children.stream()
        .map(EventLoop::isShuttingDown)
        .filter(s -> s.equals(false))
        .findAny()
        .orElse(true);
  }

  @Override
  public boolean isShutdown() {
    return this.children.stream()
        .map(EventLoop::isShutdown)
        .filter(s -> s.equals(false))
        .findAny()
        .orElse(true);
  }

  @Override
  public boolean isTerminated() {
    return this.children.stream()
        .map(EventLoop::isTerminated)
        .filter(s -> s.equals(false))
        .findAny()
        .orElse(true);
  }

  /** * Wait until every child {@link EventLoop} terminated through while-loop */
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    for (EventLoop el : this.children) {
      long timeLeft = deadline - System.nanoTime();
      while (timeLeft > 0 && !el.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
        timeLeft = deadline - System.nanoTime();
      }
      if (timeLeft <= 0) {
        break;
      }
    }
    return this.isTerminated();
  }

  @Override
  public <V> Promise<V> submit(Callable<V> task) {
    return this.next().submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return this.next().submit(task, result);
  }

  @Override
  public Promise<?> submit(Runnable task) {
    return this.next().submit(task);
  }

  @Override
  public void execute(Runnable command) {
    this.next().execute(command);
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
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }
}
