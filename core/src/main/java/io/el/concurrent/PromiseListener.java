package io.el.concurrent;

public interface PromiseListener<P extends Promise<?>> {

  void onComplete(P task) throws Exception;
}
