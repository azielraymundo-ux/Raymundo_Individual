import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private String referenceId;
    private List<CartItem> items;
    private double totalAmount;
    private String status;
    private LocalDateTime orderTime;

    public Order(String orderId, String referenceId, List<CartItem> items) {
        this.orderId = orderId;
        this.referenceId = referenceId;
        this.items = new ArrayList<>(items);
        this.totalAmount = calculateTotal();
        this.status = "PENDING";
        this.orderTime = LocalDateTime.now();
    }

    public void recalculateTotal() {
        this.totalAmount = calculateTotal();
    }

    private double calculateTotal() {
        double sum = 0;
        for (CartItem item : items) {
            sum += item.getMenuItem().getPrice() * item.getQuantity();
        }
        return sum;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public String getOrderId() { return orderId; }
    public String getReferenceId() { return referenceId; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) {
        this.items = new ArrayList<>(items);
        recalculateTotal();
    }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOrderTime() { return orderTime; }

    @Override
    public String toString() {
        return String.format("Order[%s | Ref: %s | Items: %d | Total: PHP %.2f | Status: %s]",
                orderId, referenceId, getTotalItemCount(), totalAmount, status);
    }
}