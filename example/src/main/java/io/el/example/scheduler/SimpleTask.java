package io.el.example.scheduler;

import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleTask implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger();

  private final String id;
  private final long delayMillis;
  private final long scheduled;

  public SimpleTask(String id, long delayMillis) {
    this.id = id;
    this.delayMillis = delayMillis;
    this.scheduled = 0L;
  }

  public SimpleTask(String id, long delayMillis, long scheduled) {
    this.id = id;
    this.delayMillis = delayMillis;
    this.scheduled = scheduled;
  }

  @Override
  public void run() {
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
    }

    if (Integer.parseInt(id) < 10) {
      LOGGER.info("[{}] Thread[{}], Task[id:  {}] - Done its task after '{}' ms ({} delay ms)", new Date(), Thread.currentThread().getName(), id, delayMillis, scheduled);
    } else {
      LOGGER.info("[{}] Thread[{}], Task[id: {}] - Done its task after '{}' ms ({} delay ms)", new Date(), Thread.currentThread().getName(), id, delayMillis, scheduled);
    }

  }
}
