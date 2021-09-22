package io.el.example;

import io.el.concurrent.SingleThreadEventLoop;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractSingleThreadEventLoopExample {
  private static final Logger LOGGER = LogManager.getLogger();

  private final SingleThreadEventLoop eventLoop;
  private int maxDelay;
  private int minDelay;
  private int taskDelay;
  private int numOfTasks;

  public AbstractSingleThreadEventLoopExample(SingleThreadEventLoop eventLoop) {
    this.eventLoop = eventLoop;
  }

  public void start() throws InterruptedException {
    long start = System.currentTimeMillis();
    long totalScheduledDelayMillis = 0L;

    for (int i = 0; i < numOfTasks; i += 1) {
      long scheduledDelayMillis = ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
      totalScheduledDelayMillis += scheduledDelayMillis;
      testEventLoop(String.valueOf(i), scheduledDelayMillis);
    }

    LOGGER.info("[{}] Thread[{}] - Executed all tasks - {} ms", new Date(), Thread.currentThread().getName(),  System.currentTimeMillis() - start);
    Thread.sleep(taskDelay * numOfTasks);

    eventLoop.shutdownGracefully(minDelay, TimeUnit.MILLISECONDS);
    LOGGER.info("[{}] Thread[{}] - Shutdown scheduler gracefully", new Date(), Thread.currentThread().getName());

    while (!eventLoop.isShutdown()) {
      // NO-OP
    }
    LOGGER.info("[{}] Total: {} ms, Scheduled: {} ms", new Date(), System.currentTimeMillis() - start, totalScheduledDelayMillis);
  }

  abstract protected void testEventLoop(String taskId, long scheduledDelayMillis);

  protected SingleThreadEventLoop eventLoop() {
    return eventLoop;
  }

  public AbstractSingleThreadEventLoopExample maxDelay(int maxDelay) {
    this.maxDelay = maxDelay;
    return this;
  }

  public AbstractSingleThreadEventLoopExample minDelay(int minDelay) {
    this.minDelay = minDelay;
    return this;
  }

  public AbstractSingleThreadEventLoopExample taskDelay(int taskDelay) {
    this.taskDelay = taskDelay;
    return this;
  }

  public AbstractSingleThreadEventLoopExample numOfTasks(int numOfTasks) {
    this.numOfTasks = numOfTasks;
    return this;
  }

  public int maxDelay() {
    return maxDelay;
  }

  public int minDelay() {
    return minDelay;
  }

  public int taskDelay() {
    return taskDelay;
  }

  public int numOfTasks() {
    return numOfTasks;
  }
}
