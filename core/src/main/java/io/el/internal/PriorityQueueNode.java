package io.el.internal;

public interface PriorityQueueNode {

  int PRIORITY_NOT_IN_QUEUE = -1;
  int INDEX_NOT_IN_QUEUE = -1;

  int index();

  void index(int i);
}
