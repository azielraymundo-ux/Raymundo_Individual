import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PriorityWaitQueue implements Serializable {
    private static final long serialVersionUID = 1L;

    private static class PriorityNode implements Serializable {
        private static final long serialVersionUID = 1L;
        WalkinGuest guest;
        int priority;
        PriorityNode next;

        PriorityNode(WalkinGuest guest, int priority) {
            this.guest = guest;
            this.priority = priority;
            this.next = null;
        }
    }

    private PriorityNode head;
    private int size;

    public PriorityWaitQueue() {
        this.head = null;
        this.size = 0;
    }

    public boolean isEmpty() { return head == null; }

    public void enqueue(WalkinGuest guest) {
        int defaultPriority = guest.isVip() ? 10 : 1;
        enqueue(guest, defaultPriority);
    }

    public void enqueue(WalkinGuest guest, int priority) {
        PriorityNode newNode = new PriorityNode(guest, priority);
        if (isEmpty() || head.priority < priority) {
            newNode.next = head;
            head = newNode;
        } else {
            PriorityNode current = head;
            while (current.next != null && current.next.priority >= priority) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        }
        size++;
    }

    public WalkinGuest dequeue() {
        if (isEmpty()) return null;
        WalkinGuest guest = head.guest;
        head = head.next;
        size--;
        return guest;
    }

    public WalkinGuest peek() {
        return isEmpty() ? null : head.guest;
    }

    public boolean remove(String guestId) {
        if (isEmpty()) return false;
        if (head.guest.getGuestId().equalsIgnoreCase(guestId)) {
            head = head.next;
            size--;
            return true;
        }
        PriorityNode current = head;
        while (current.next != null) {
            if (current.next.guest.getGuestId().equalsIgnoreCase(guestId)) {
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public List<WalkinGuest> getAll() {
        List<WalkinGuest> list = new ArrayList<>();
        PriorityNode current = head;
        while (current != null) {
            list.add(current.guest);
            current = current.next;
        }
        return list;
    }

    public int size() { return size; }
    public void clear() { head = null; size = 0; }
}