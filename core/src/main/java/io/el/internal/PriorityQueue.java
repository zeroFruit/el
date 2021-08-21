package io.el.internal;

import java.util.Queue;

public interface PriorityQueue<T> extends Queue<T> {

  boolean removeTyped(T node);

  boolean containsTyped(T node);

  void changePriority(T node);
}
