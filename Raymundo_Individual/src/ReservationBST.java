import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationBST implements Serializable {
    private static final long serialVersionUID = 1L;

    private BSTNode<Reservation> root;
    private int size;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void insert(Reservation r) {
        root = insert(root, r);
    }

    private BSTNode<Reservation> insert(BSTNode<Reservation> node, Reservation r) {
        if (node == null) {
            size++;
            return new BSTNode<>(r);
        }
        int c = r.compareTo(node.data);
        if (c < 0) node.left = insert(node.left, r);
        else if (c > 0) node.right = insert(node.right, r);
        else throw new IllegalArgumentException("Duplicate reservation: " + r.getReservationId());
        return node;
    }

    public boolean remove(Reservation r) {
        int before = size;
        root = remove(root, r);
        return size < before;
    }

    private BSTNode<Reservation> remove(BSTNode<Reservation> node, Reservation r) {
        if (node == null) return null;
        int c = r.compareTo(node.data);
        if (c < 0) {
            node.left = remove(node.left, r);
        } else if (c > 0) {
            node.right = remove(node.right, r);
        } else {
            if (node.left == null) { size--; return node.right; }
            if (node.right == null) { size--; return node.left; }
            BSTNode<Reservation> min = node.right;
            while (min.left != null) min = min.left;
            node.data = min.data;
            node.right = remove(node.right, min.data);
        }
        return node;
    }

    public List<Reservation> inOrder() {
        List<Reservation> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    private void inOrder(BSTNode<Reservation> node, List<Reservation> out) {
        if (node == null) return;
        inOrder(node.left, out);
        out.add(node.data);
        inOrder(node.right, out);
    }

    public Reservation findById(String reservationId) {
        for (Reservation r : inOrder()) {
            if (r.getReservationId().equalsIgnoreCase(reservationId)) return r;
        }
        return null;
    }

    public List<Reservation> getBetween(LocalDateTime from, LocalDateTime to) {
        List<Reservation> out = new ArrayList<>();
        collect(root, from, to, out);
        return out;
    }

    private void collect(BSTNode<Reservation> node, LocalDateTime from, LocalDateTime to, List<Reservation> out) {
        if (node == null) return;
        LocalDateTime t = node.data.getDateTime();
        if (!t.isBefore(from)) collect(node.left, from, to, out);
        if (!t.isBefore(from) && t.isBefore(to)) out.add(node.data);
        if (t.isBefore(to)) collect(node.right, from, to, out);
    }

    public Reservation findConflict(Reservation candidate) {
        return findConflict(root, candidate);
    }

    private Reservation findConflict(BSTNode<Reservation> node, Reservation c) {
        if (node == null) return null;
        Reservation fromLeft = findConflict(node.left, c);
        if (fromLeft != null) return fromLeft;

        Reservation existing = node.data;
        if (existing.getStatus() != ReservationStatus.CANCELLED
                && !existing.getReservationId().equals(c.getReservationId())
                && existing.overlaps(c)) {
            return existing;
        }
        if (existing.getDateTime().isBefore(c.getEndTime())) {
            return findConflict(node.right, c);
        }
        return null;
    }
}