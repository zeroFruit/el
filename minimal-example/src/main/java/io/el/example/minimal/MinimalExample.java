package io.el.example.minimal;

import io.el.concurrent.DefaultSingleThreadEventLoop;
import io.el.concurrent.EventLoop;
import io.el.concurrent.ThreadPerTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MinimalExample {

  public static void main(String[] args) {
    Executor executor = new ThreadPerTaskExecutor(
        Executors.defaultThreadFactory());
    EventLoop eventLoop = new DefaultSingleThreadEventLoop(executor);

    eventLoop.schedule(() -> {
      System.out.println("print this message 100 secs later");
    }, 100, TimeUnit.MILLISECONDS);

    eventLoop.shutdownGracefully(1, TimeUnit.SECONDS);
  }
}
