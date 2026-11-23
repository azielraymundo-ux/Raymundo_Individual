import java.io.Serializable;

class BSTNode<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    T data;
    BSTNode<T> left;
    BSTNode<T> right;

    BSTNode(T data) {
        this.data = data;
    }
}