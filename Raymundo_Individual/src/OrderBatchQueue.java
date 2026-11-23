import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class OrderBatchQueue implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Queue<Order> pendingOrders;
    private final Queue<List<Order>> readyBatches;
    private final int batchSizeThreshold;

    public OrderBatchQueue(int batchSizeThreshold) {
        this.pendingOrders = new LinkedList<>();
        this.readyBatches = new LinkedList<>();
        this.batchSizeThreshold = batchSizeThreshold;
    }

    public boolean addOrderToEngine(Order order) {
        order.setStatus("PENDING");
        pendingOrders.offer(order);
        if (pendingOrders.size() >= batchSizeThreshold) {
            createBatch();
            return true;
        }
        return false;
    }

    public List<Order> createBatch() {
        if (pendingOrders.isEmpty()) {
            return new ArrayList<>();
        }
        List<Order> batch = new ArrayList<>();
        while (!pendingOrders.isEmpty() && batch.size() < batchSizeThreshold) {
            Order o = pendingOrders.poll();
            o.setStatus("BATCHED");
            batch.add(o);
        }
        readyBatches.offer(batch);
        return batch;
    }

    public Map<String, Integer> processNextBatch() {
        List<Order> nextBatch = readyBatches.poll();
        Map<String, Integer> aggregatedKitchenTicket = new HashMap<>();

        if (nextBatch == null) {
            return aggregatedKitchenTicket;
        }

        for (Order order : nextBatch) {
            order.setStatus("PREPARING");
            for (CartItem item : order.getItems()) {
                String itemName = item.getMenuItem().getName();
                aggregatedKitchenTicket.put(
                        itemName,
                        aggregatedKitchenTicket.getOrDefault(itemName, 0) + item.getQuantity()
                );
            }
        }
        return aggregatedKitchenTicket;
    }

    public int getPendingCount() { return pendingOrders.size(); }
    public int getReadyBatchCount() { return readyBatches.size(); }
}