package io.el.example.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleTask implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();

  private final String id;
  private final long delayMillis;

  public SimpleTask(String id, long delayMillis) {
    this.id = id;
    this.delayMillis = delayMillis;
  }

  @Override
  public void run() {
    if (Integer.parseInt(id) < 10) {
      LOGGER.info("Thread[{}], Task[id:  {}] - Done its task after '{}' ms", Thread.currentThread().getName(), id, delayMillis);
    } else {
      LOGGER.info("Thread[{}], Task[id: {}] - Done its task after '{}' ms", Thread.currentThread().getName(), id, delayMillis);
    }

  }
}
