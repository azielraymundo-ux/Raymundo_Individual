import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomerHashMap implements Serializable {
    private static final long serialVersionUID = 1L;

    private static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        final String key;
        Customer value;
        Entry next;

        Entry(String key, Customer value, Entry next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int INITIAL_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    private Entry[] buckets;
    private int size;

    public CustomerHashMap() {
        buckets = new Entry[INITIAL_CAPACITY];
        size = 0;
    }

    private int indexFor(String key, int length) {
        return (key.hashCode() & 0x7fffffff) % length;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public boolean containsKey(String key) { return get(key) != null; }

    public Customer get(String key) {
        if (key == null) return null;
        for (Entry e = buckets[indexFor(key, buckets.length)]; e != null; e = e.next) {
            if (e.key.equals(key)) return e.value;
        }
        return null;
    }

    public Customer put(String key, Customer value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        int i = indexFor(key, buckets.length);
        for (Entry e = buckets[i]; e != null; e = e.next) {
            if (e.key.equals(key)) {
                Customer old = e.value;
                e.value = value;
                return old;
            }
        }
        buckets[i] = new Entry(key, value, buckets[i]);
        size++;
        if (size > buckets.length * LOAD_FACTOR) resize();
        return null;
    }

    public Customer remove(String key) {
        if (key == null) return null;
        int i = indexFor(key, buckets.length);
        Entry prev = null;
        for (Entry e = buckets[i]; e != null; prev = e, e = e.next) {
            if (e.key.equals(key)) {
                if (prev == null) buckets[i] = e.next;
                else prev.next = e.next;
                size--;
                return e.value;
            }
        }
        return null;
    }

    public List<Customer> values() {
        List<Customer> out = new ArrayList<>();
        for (Entry head : buckets) {
            for (Entry e = head; e != null; e = e.next) {
                out.add(e.value);
            }
        }
        return out;
    }

    private void resize() {
        Entry[] old = buckets;
        buckets = new Entry[old.length * 2];
        for (Entry head : old) {
            for (Entry e = head; e != null; e = e.next) {
                int i = indexFor(e.key, buckets.length);
                buckets[i] = new Entry(e.key, e.value, buckets[i]);
            }
        }
    }
}