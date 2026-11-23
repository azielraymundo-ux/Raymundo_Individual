import java.io.Serializable;

public class QueueNode<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public T data;
    public QueueNode<T> next;

    public QueueNode(T data) {
        this.data = data;
        this.next = null;
    }
}