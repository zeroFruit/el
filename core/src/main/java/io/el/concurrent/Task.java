package io.el.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface Task<V> extends Future<V> {

  boolean isSuccess();

  Task<V> setSuccess(V result);

  Task<V> addListener(TaskListener<? extends Task<? super V>> listener);

  Task<V> await(long timeout, TimeUnit unit) throws InterruptedException;

  Task<V> await() throws InterruptedException;

  Task<V> setFailure(Throwable cause);
}