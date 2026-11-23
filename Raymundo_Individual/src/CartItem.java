import java.io.Serializable;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private MenuItem menuItem;
    private int quantity;
    private String specialRequests;

    public CartItem(MenuItem menuItem, int quantity, String specialRequests) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.specialRequests = specialRequests;
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public double getTotalPrice() {
        return menuItem.getPrice() * quantity;
    }
}