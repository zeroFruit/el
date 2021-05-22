package io.el.internal;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import static io.el.internal.ObjectUtil.checkNotNull;
import static io.el.internal.PriorityQueueNode.INDEX_NOT_IN_QUEUE;
import static io.el.internal.PriorityQueueNode.PRIORITY_NOT_IN_QUEUE;

public class DefaultPriorityQueue<T extends PriorityQueueNode> extends AbstractQueue<T> implements PriorityQueue<T> {
    private static final PriorityQueueNode[] EMPTY_ARRAY = new PriorityQueueNode[0];
    private final Comparator<T> comparator;
    private T[] items;
    private int size;

    @SuppressWarnings("unchecked")
    public DefaultPriorityQueue(Comparator<T> comparator, int initialSize) {
        this.comparator = checkNotNull(comparator, "comparator");
        this.items = (T[]) (initialSize != 0 ? new PriorityQueueNode[initialSize] : EMPTY_ARRAY);
    }

    // TODO
    @Override
    public Iterator<T> iterator() {
        return null;
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
            // Use a policy which allows for a 0 initial capacity. Same policy as JDK's priority queue, double when
            // "small", then grow by 50% when "large".
            items = Arrays.copyOf(items, items.length + ((items.length < 64) ?
                    (items.length + 2) :
                    (items.length >>> 1)));
        }

        items[size] = node;

        int current = size;
        while (node.priority() < parent(current).priority()) {
            int parentIndex = parent(current).index();
            swap(current, parentIndex);
            current = parentIndex;
        }

        node.index(current);
        size += 1;

        return true;
    }

    @Override
    public T poll() {
        if (size == 0) {
            return null;
        }
        T result = items[0];
        result.prioritize(PRIORITY_NOT_IN_QUEUE);
        result.index(INDEX_NOT_IN_QUEUE);
        size -= 1;

        T last = items[size];
        items[size] = null;
        items[0] = last;
        last.index(0);
        if (size != 0) {
            heapify(0);
        }
        return result;
    }

    @Override
    public T peek() {
        return (size == 0) ? null : items[0];
    }

    // TODO
    @Override
    public boolean removeTyped(T node) {
        return false;
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

    // TODO
    @Override
    public void changePriority(T node) {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(items[i]).append("\n");
        }
        return sb.toString();
    }

    private void heapify(int i) {
        T node = items[i];

        if (isLeaf(node)) {
            return;
        }

        T left = leftChild(node);
        T right = rightChild(node);
        int priority = node.priority();

        if (left == null && priority > right.priority()) {
            swap(node.index(), right.index());
            heapify(node.index());
            return;
        }

        if (right == null && priority > left.priority()) {
            swap(node.index(), left.index());
            heapify(node.index());
            return;
        }

        if (priority <= left.priority() && priority <= right.priority()) {
            return;
        }
        if (left.priority() < right.priority()) {
            swap(node.index(), left.index());
            heapify(node.index());
            return;
        }
        swap(node.index(), right.index());
        heapify(node.index());
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
}
