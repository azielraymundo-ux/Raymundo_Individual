import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CustomerHashMap customersById;
    private final Map<String, Customer> customersByEmail;
    private final Map<String, Customer> customersByPhone;
    private final Map<String, String> credentials;
    private int customerCounter;

    public CustomerManager() {
        this.customersById = new CustomerHashMap();
        this.customersByEmail = new HashMap<>();
        this.customersByPhone = new HashMap<>();
        this.credentials = new HashMap<>();
        this.customerCounter = 1;
    }

    public void importAccount(Customer c) {
        if (c == null) return;
        customersById.put(c.getCustomerId(), c);
        if (c.getEmail() != null && !c.getEmail().isBlank()) {
            String normEmail = c.getEmail().trim().toLowerCase();
            customersByEmail.put(normEmail, c);
            credentials.put(normEmail, c.getPassword());
        }
        if (c.getPhone() != null && !c.getPhone().isBlank()) {
            customersByPhone.put(c.getPhone().trim(), c);
        }
        if (c.getCustomerId().startsWith("C")) {
            try {
                int num = Integer.parseInt(c.getCustomerId().substring(1));
                if (num >= customerCounter) customerCounter = num + 1;
            } catch (NumberFormatException ignored) {}
        }
    }

    public Customer registerAccount(String name, String email, String phone, String password) {
        return registerAccount(name, email, phone, password, "CUSTOMER");
    }

    public Customer registerAccount(String name, String email, String phone, String password, String role) {
        if (email == null || email.isBlank() || customersByEmail.containsKey(email.trim().toLowerCase())) {
            throw new IllegalArgumentException("Invalid email or email is already registered.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        if (phone != null && !phone.isBlank() && customersByPhone.containsKey(phone.trim())) {
            throw new IllegalArgumentException("Phone number is already associated with another account.");
        }

        String prefix = "MANAGER".equalsIgnoreCase(role) ? "MGR" : "C";
        String id = String.format("%s%03d", prefix, customerCounter++);
        String normalizedEmail = email.trim().toLowerCase();
        Customer customer = new Customer(id, name.trim(), normalizedEmail, password, role,
                phone != null ? phone.trim() : "", "MANAGER".equalsIgnoreCase(role));

        customersById.put(id, customer);
        customersByEmail.put(normalizedEmail, customer);
        if (phone != null && !phone.isBlank()) {
            customersByPhone.put(phone.trim(), customer);
        }
        credentials.put(normalizedEmail, password);

        DataPersistenceService.saveCredentialsToJson(getAllCustomers());
        return customer;
    }

    public Customer login(String email, String password) {
        if (email == null || password == null) return null;
        String normalizedEmail = email.trim().toLowerCase();
        if (credentials.containsKey(normalizedEmail) && credentials.get(normalizedEmail).equals(password)) {
            return customersByEmail.get(normalizedEmail);
        }
        return null;
    }

    public Customer findById(String customerId) { return customersById.get(customerId); }

    public Customer findByEmail(String email) {
        return email == null ? null : customersByEmail.get(email.trim().toLowerCase());
    }

    public Customer findByPhone(String phone) {
        return phone == null ? null : customersByPhone.get(phone.trim());
    }

    public void recordCustomerVisit(String customerId) {
        Customer c = findById(customerId);
        if (c != null) {
            c.recordVisit();
            if (c.getVisitCount() >= 5) c.setVip(true);
            DataPersistenceService.saveCredentialsToJson(getAllCustomers());
        }
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customersById.values());
    }

    public boolean removeCustomer(String customerId) {
        Customer c = customersById.remove(customerId);
        if (c != null) {
            if (c.getPhone() != null) customersByPhone.remove(c.getPhone());
            customersByEmail.values().removeIf(cust -> cust.getCustomerId().equals(customerId));
            DataPersistenceService.saveCredentialsToJson(getAllCustomers());
            return true;
        }
        return false;
    }
}