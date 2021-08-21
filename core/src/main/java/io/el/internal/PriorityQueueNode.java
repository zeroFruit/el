package io.el.internal;

public interface PriorityQueueNode {

  int PRIORITY_NOT_IN_QUEUE = -1;
  int INDEX_NOT_IN_QUEUE = -1;

  int priority();

  void prioritize(int i);

  int index();

  void index(int i);
}
