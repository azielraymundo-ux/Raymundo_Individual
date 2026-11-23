import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WaitQueue<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private QueueNode<T> front;
    private QueueNode<T> rear;
    private int size;

    public WaitQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    public boolean isEmpty() { return front == null; }

    public void enqueue(T item) {
        QueueNode<T> newNode = new QueueNode<>(item);
        if (rear == null) {
            front = rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }

    public T dequeue() {
        if (isEmpty()) return null;
        T item = front.data;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return item;
    }

    public T peek() { return isEmpty() ? null : front.data; }
    public int size() { return size; }
    public void clear() { front = null; rear = null; size = 0; }

    public List<T> toList() {
        List<T> out = new ArrayList<>();
        for (QueueNode<T> n = front; n != null; n = n.next) {
            out.add(n.data);
        }
        return out;
    }
}