package io.el.internal;

import static io.el.internal.PriorityQueueNode.INDEX_NOT_IN_QUEUE;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DefaultPriorityQueue<T extends PriorityQueueNode> extends AbstractQueue<T>
    implements PriorityQueue<T> {

  private static final PriorityQueueNode[] EMPTY_ARRAY = new PriorityQueueNode[0];

  private final Comparator<T> comparator;

  private T[] items;
  private int size;

  @SuppressWarnings("unchecked")
  public DefaultPriorityQueue(int initialSize, Comparator<T> comparator) {
    this.items = (T[]) (initialSize != 0 ? new PriorityQueueNode[initialSize] : EMPTY_ARRAY);
    this.comparator = comparator;
  }

  @Override
  public Iterator<T> iterator() {
    return new PriorityQueueIterator();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean offer(T node) {
    if (node.index() != INDEX_NOT_IN_QUEUE) {
      throw new IllegalArgumentException("Invalid node index: " + node.index());
    }
    // Check that the array capacity is enough to hold values by doubling capacity.
    if (size >= items.length) {
      // Use a policy which allows for a 0 initial capacity. Same policy as JDK's priority queue,
      // double when
      // "small", then grow by 50% when "large".
      items =
          Arrays.copyOf(
              items,
              items.length + ((items.length < 64) ? (items.length + 2) : (items.length >>> 1)));
    }

    items[size] = node;

    int current = size;
    while (comparator.compare(node, parent(current)) < 0) {
      int parentIndex = parent(current).index();
      swap(current, parentIndex);
      current = parentIndex;
    }

    node.index(current);
    size += 1;

    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T poll() {
    if (size == 0) {
      return null;
    }
    T result = items[0];
    result.index(INDEX_NOT_IN_QUEUE);
    size -= 1;

    T last = items[size];
    items[size] = null;

    if (size == 0) {
      items = (T[]) EMPTY_ARRAY;
      return last;
    }
    items[0] = last;
    last.index(0);
    bubbleDown(0);
    return result;
  }

  @Override
  public T peek() {
    return (size == 0) ? null : items[0];
  }

  @Override
  public boolean remove(Object e) {
    if (!(e instanceof PriorityQueueNode)) {
      return false;
    }
    return remove((PriorityQueueNode) e);
  }

  @Override
  public T remove() {
    T item = items[0];
    remove(item);
    return item;
  }

  @Override
  public boolean removeTyped(T node) {
    return remove(node);
  }

  @SuppressWarnings("unchecked")
  private boolean remove(PriorityQueueNode node) {
    if (!contains(node)) {
      return false;
    }
    if (size == 1) {
      items = (T[]) EMPTY_ARRAY;
      node.index(INDEX_NOT_IN_QUEUE);
      size = 0;
      return true;
    }

    int indexRemoved = node.index();
    node.index(INDEX_NOT_IN_QUEUE);

    T moved = items[size - 1];
    items[indexRemoved] = moved;
    moved.index(indexRemoved);

    size -= 1;
    bubbleDown(moved.index());
    return true;
  }

  @Override
  public boolean contains(Object e) {
    if (!(e instanceof PriorityQueueNode)) {
      return false;
    }
    PriorityQueueNode node = (PriorityQueueNode) e;
    return contains(node);
  }

  @Override
  public boolean containsTyped(T node) {
    return contains(node);
  }

  private boolean contains(PriorityQueueNode node) {
    return node.index() != INDEX_NOT_IN_QUEUE && node.equals(items[node.index()]);
  }

  @Override
  public void changePriority(T node) {
    if (!contains(node)) {
      return;
    }
    if (node.index() == 0) {
      bubbleDown(0);
      return;
    }
    if (comparator.compare(node, parent(node.index())) < 0) {
      bubbleUp(node);
      return;
    }
    bubbleDown(node.index());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      sb.append(items[i]).append("\n");
    }
    return sb.toString();
  }

  private void bubbleDown(int i) {
    T node = items[i];

    if (isLeaf(node)) {
      return;
    }

    T left = leftChild(node);
    T right = rightChild(node);

    if (left == null) {
      if (comparator.compare(node, right) <= 0) {
        return;
      }
      swap(node.index(), right.index());
      bubbleDown(node.index());
      return;
    }

    if (right == null) {
      if (comparator.compare(node, left) <= 0) {
        return;
      }
      swap(node.index(), left.index());
      bubbleDown(node.index());
      return;
    }

    if (comparator.compare(node, left) <= 0 && comparator.compare(node, right) <= 0) {
      return;
    }
    if (comparator.compare(left, right) < 0) {
      swap(node.index(), left.index());
      bubbleDown(node.index());
      return;
    }
    swap(node.index(), right.index());
    bubbleDown(node.index());
  }

  private void bubbleUp(T node) {
    int current = node.index();
    while (comparator.compare(node, parent(current)) < 0) {
      int parentIndex = parent(current).index();
      swap(current, parentIndex);
      current = parentIndex;
    }

    node.index(current);
  }

  private boolean isLeaf(T node) {
    int idx = node.index();
    return idx >= (size / 2) && idx <= size;
  }

  private T parent(int i) {
    return items[i / 2];
  }

  private T leftChild(T node) {
    return items[(node.index() * 2) + 1];
  }

  private T rightChild(T node) {
    return items[(node.index() * 2) + 2];
  }

  private void swap(int x, int y) {
    T tmp;
    tmp = items[x];
    items[x] = items[y];
    items[y] = tmp;

    items[x].index(x);
    items[y].index(y);
  }

  private final class PriorityQueueIterator implements Iterator<T> {

    private int index;

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public T next() {
      if (index >= size) {
        throw new NoSuchElementException();
      }
      T result = items[index];
      index += 1;
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
