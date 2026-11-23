import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuOrderManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<MenuItem> digitalMenu;
    private final Map<String, List<CartItem>> activeCarts;
    private final List<Order> orderHistory;
    private final OrderBatchQueue batchEngine;
    private int orderCounter = 1;
    private int menuCounter = 1;

    public MenuOrderManager(int batchThreshold) {
        this.digitalMenu = new ArrayList<>();
        this.activeCarts = new HashMap<>();
        this.orderHistory = new ArrayList<>();
        this.batchEngine = new OrderBatchQueue(batchThreshold);
        seedDefaultMenu();
    }

    private void seedDefaultMenu() {
        digitalMenu.clear();
        menuCounter = 1;

        // 5 Appetizers
        addMenuItem(new MenuItem("M01", "Crispy Calamari", "Appetizer", 290.00, "Golden-fried squid rings with garlic aioli"));
        addMenuItem(new MenuItem("M02", "Baked Spinach Dip", "Appetizer", 260.00, "Creamy spinach and parmesan dip with toasted ciabatta"));
        addMenuItem(new MenuItem("M03", "Beef Sisig Sliders", "Appetizer", 320.00, "Savory sizzling sisig on brioche buns with calamansi aioli"));
        addMenuItem(new MenuItem("M04", "Bruschetta Trio", "Appetizer", 240.00, "Heirloom tomato, pesto burrata, and mushroom tapenade"));
        addMenuItem(new MenuItem("M05", "Garlic Shrimp Skewers", "Appetizer", 340.00, "Char-grilled tiger prawns with lemon herb butter"));

        // 5 Main Courses
        addMenuItem(new MenuItem("M06", "Garlic Butter Ribeye", "Main Course", 850.00, "12oz ribeye seared in roasted garlic herb butter"));
        addMenuItem(new MenuItem("M07", "Pan-Seared Salmon", "Main Course", 620.00, "Herb butter salmon fillet with roasted asparagus"));
        addMenuItem(new MenuItem("M08", "Truffle Chicken Risotto", "Main Course", 540.00, "Arborio rice slow-cooked with wild mushrooms and truffle oil"));
        addMenuItem(new MenuItem("M09", "Braised Pork Belly", "Main Course", 490.00, "Slow-braised pork belly with sweet soy glaze"));
        addMenuItem(new MenuItem("M10", "Herb Roast Half Chicken", "Main Course", 510.00, "Rosemary and lemon roasted chicken with pan jus"));

        // 5 Soups
        addMenuItem(new MenuItem("M11", "Truffle Mushroom Soup", "Soup", 210.00, "Velvety roasted mushroom soup finished with truffle cream"));
        addMenuItem(new MenuItem("M12", "Seafood Bouillabaisse", "Soup", 310.00, "Aromatic tomato-saffron broth with mussels, shrimp, and fish"));
        addMenuItem(new MenuItem("M13", "Roasted Tomato Bisque", "Soup", 185.00, "Fire-roasted tomatoes blended with sweet basil and cream"));
        addMenuItem(new MenuItem("M14", "Spiced Pumpkin Veloute", "Soup", 190.00, "Smooth spiced squash soup with toasted pumpkin seeds"));
        addMenuItem(new MenuItem("M15", "Classic Chicken Noodle", "Soup", 175.00, "Slow-simmered chicken broth with egg noodles and mirepoix"));

        // 5 Salads
        addMenuItem(new MenuItem("M16", "Classic Caesar Salad", "Salad", 250.00, "Crisp romaine, shaved parmesan, herb croutons, Caesar dressing"));
        addMenuItem(new MenuItem("M17", "Burrata & Heirloom Salad", "Salad", 330.00, "Creamy burrata, vine tomatoes, arugula, balsamic glaze"));
        addMenuItem(new MenuItem("M18", "Mango Pomelo Greens", "Salad", 245.00, "Mixed greens, ripe mango, pomelo segments, citrus vinaigrette"));
        addMenuItem(new MenuItem("M19", "Mediterranean Greek Salad", "Salad", 265.00, "Cucumber, kalamata olives, feta cheese, red onion, oregano"));
        addMenuItem(new MenuItem("M20", "Seared Tuna Nicoise", "Salad", 350.00, "Seared tuna, baby potatoes, green beans, egg, herb dressing"));

        // 5 Sides
        addMenuItem(new MenuItem("M21", "Truffle Parmesan Fries", "Side", 195.00, "Crispy shoestring fries tossed in truffle oil and parmesan"));
        addMenuItem(new MenuItem("M22", "Creamy Garlic Mash", "Side", 150.00, "Whipped Yukon potatoes with roasted garlic and butter"));
        addMenuItem(new MenuItem("M23", "Charred Asparagus", "Side", 180.00, "Flame-kissed asparagus spears with sea salt and lemon"));
        addMenuItem(new MenuItem("M24", "Garlic Herb Rice", "Side", 95.00, "Fragrant jasmine rice sauteed with garlic and parsley"));
        addMenuItem(new MenuItem("M25", "Sauteed Wild Mushrooms", "Side", 185.00, "Shiitake and oyster mushrooms sauteed with thyme"));

        // 5 Desserts
        addMenuItem(new MenuItem("M26", "Classic Tiramisu", "Dessert", 240.00, "Espresso-soaked ladyfingers layered with mascarpone cream"));
        addMenuItem(new MenuItem("M27", "Molten Lava Cake", "Dessert", 250.00, "Warm dark chocolate cake with vanilla bean ice cream"));
        addMenuItem(new MenuItem("M28", "Mango Sans Rival", "Dessert", 230.00, "Cashew meringue layers with buttercream and ripe mango"));
        addMenuItem(new MenuItem("M29", "Basque Burnt Cheesecake", "Dessert", 260.00, "Caramelized crust cheesecake with a soft, creamy center"));
        addMenuItem(new MenuItem("M30", "Matcha Panna Cotta", "Dessert", 210.00, "Silky ceremonial matcha cream topped with red bean"));
    }

    public void replaceMenu(List<MenuItem> items) {
        digitalMenu.clear();
        menuCounter = 1;
        if (items != null) {
            for (MenuItem item : items) addMenuItem(item);
        }
    }

    public String generateNextMenuItemId() {
        while (true) {
            String candidate = String.format("M%02d", menuCounter++);
            if (findMenuItemById(candidate) == null) {
                return candidate;
            }
        }
    }

    public void addMenuItem(MenuItem item) {
        if (item == null) return;
        digitalMenu.add(item);
        if (item.getId() != null && item.getId().toUpperCase().startsWith("M")) {
            try {
                int num = Integer.parseInt(item.getId().substring(1));
                if (num >= menuCounter) menuCounter = num + 1;
            } catch (NumberFormatException ignored) {}
        }
    }

    public boolean editMenuItem(String itemId, String newName, String newCategory, double newPrice, String newDescription) {
        MenuItem item = findMenuItemById(itemId);
        if (item == null || newPrice < 0) return false;
        int idx = digitalMenu.indexOf(item);
        MenuItem updated = new MenuItem(
                item.getId(),
                (newName == null || newName.isBlank()) ? item.getName() : newName.trim(),
                (newCategory == null || newCategory.isBlank()) ? item.getCategory() : newCategory.trim(),
                newPrice,
                (newDescription == null) ? item.getDescription() : newDescription.trim()
        );
        digitalMenu.set(idx, updated);
        return true;
    }

    public boolean deleteMenuItem(String itemId) {
        return digitalMenu.removeIf(item -> item.getId().equalsIgnoreCase(itemId));
    }

    public List<MenuItem> getAllMenuItems() { return new ArrayList<>(digitalMenu); }

    public List<MenuItem> getMenuByCategory(String category) {
        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem item : digitalMenu) {
            if (item.getCategory().equalsIgnoreCase(category)) filtered.add(item);
        }
        return filtered;
    }

    public MenuItem findMenuItemById(String itemId) {
        if (itemId == null) return null;
        for (MenuItem item : digitalMenu) {
            if (item.getId().equalsIgnoreCase(itemId)) return item;
        }
        return null;
    }

    public void addToCart(String referenceId, String menuItemId, int quantity) {
        addToCart(referenceId, menuItemId, quantity, "");
    }

    public void addToCart(String referenceId, String menuItemId, int quantity, String specialRequests) {
        MenuItem menuItem = findMenuItemById(menuItemId);
        if (menuItem == null || quantity <= 0) return;

        List<CartItem> cart = activeCarts.computeIfAbsent(referenceId, k -> new ArrayList<>());
        String normalizedRequest = (specialRequests == null) ? "" : specialRequests.trim();

        for (CartItem existing : cart) {
            String existingReq = (existing.getSpecialRequests() == null) ? "" : existing.getSpecialRequests().trim();
            if (existing.getMenuItem().getId().equalsIgnoreCase(menuItemId)
                    && existingReq.equalsIgnoreCase(normalizedRequest)) {
                existing.setQuantity(existing.getQuantity() + quantity);
                return;
            }
        }
        cart.add(new CartItem(menuItem, quantity, normalizedRequest));
    }

    public void removeFromCart(String referenceId, String menuItemId) {
        List<CartItem> cart = activeCarts.get(referenceId);
        if (cart != null) cart.removeIf(item -> item.getMenuItem().getId().equalsIgnoreCase(menuItemId));
    }

    public boolean removeFromCart(String referenceId, String menuItemId, int quantityToRemove) {
        List<CartItem> cart = activeCarts.get(referenceId);
        if (cart == null || quantityToRemove <= 0) return false;

        for (int i = 0; i < cart.size(); i++) {
            CartItem item = cart.get(i);
            if (item.getMenuItem().getId().equalsIgnoreCase(menuItemId)) {
                int remaining = item.getQuantity() - quantityToRemove;
                if (remaining <= 0) {
                    cart.remove(i);
                } else {
                    item.setQuantity(remaining);
                }
                return true;
            }
        }
        return false;
    }

    public List<CartItem> getCart(String referenceId) {
        return activeCarts.getOrDefault(referenceId, new ArrayList<>());
    }

    public double getCartTotal(String referenceId) {
        double total = 0;
        for (CartItem item : getCart(referenceId)) total += item.getTotalPrice();
        return total;
    }

    public void clearCart(String referenceId) { activeCarts.remove(referenceId); }

    public Order checkoutCart(String referenceId) {
        List<CartItem> cart = activeCarts.get(referenceId);
        if (cart == null || cart.isEmpty()) return null;

        String orderId = String.format("ORD-%03d", orderCounter++);
        Order newOrder = new Order(orderId, referenceId, cart);
        orderHistory.add(newOrder);
        batchEngine.addOrderToEngine(newOrder);
        clearCart(referenceId);
        return newOrder;
    }

    public Order findOrderById(String orderId) {
        if (orderId == null) return null;
        for (Order o : orderHistory) {
            if (o.getOrderId().equalsIgnoreCase(orderId)) return o;
        }
        return null;
    }

    public boolean editOrder(String orderId, String menuItemId, int newQuantity) {
        Order order = findOrderById(orderId);
        if (order == null || !"PENDING".equalsIgnoreCase(order.getStatus())) {
            return false;
        }
        MenuItem menuItem = findMenuItemById(menuItemId);
        if (menuItem == null) return false;

        List<CartItem> items = new ArrayList<>(order.getItems());
        boolean found = false;
        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            if (ci.getMenuItem().getId().equalsIgnoreCase(menuItemId)) {
                found = true;
                if (newQuantity <= 0) {
                    items.remove(i);
                } else {
                    ci.setQuantity(newQuantity);
                }
                break;
            }
        }
        if (!found && newQuantity > 0) {
            items.add(new CartItem(menuItem, newQuantity, ""));
        }
        if (items.isEmpty()) return false;

        order.setItems(items);
        return true;
    }

    public OrderBatchQueue getBatchEngine() { return batchEngine; }
    public List<Order> getOrderHistory() { return new ArrayList<>(orderHistory); }
}