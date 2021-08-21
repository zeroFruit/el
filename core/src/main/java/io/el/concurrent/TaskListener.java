package io.el.concurrent;

public interface TaskListener<P extends Task<?>> {

  void onComplete(P task) throws Exception;
}
