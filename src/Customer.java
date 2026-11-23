import java.io.Serializable;

public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String customerId;
    private String name;
    private String email;
    private String password;
    private String role;
    private String phone;
    private int visitCount;
    private boolean vip;

    public Customer(String customerId, String name, String phone) {
        this(customerId, name, "", "", "CUSTOMER", phone, false);
    }

    public Customer(String customerId, String name, String phone, boolean vip) {
        this(customerId, name, "", "", "CUSTOMER", phone, vip);
    }

    public Customer(String customerId, String name, String email, String password, String role, String phone, boolean vip) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.customerId = customerId;
        this.name = name;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
        this.role = (role == null || role.isBlank()) ? "CUSTOMER" : role.toUpperCase();
        this.phone = phone == null ? "" : phone.trim();
        this.vip = vip;
        this.visitCount = 0;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public boolean isManager() { return "MANAGER".equalsIgnoreCase(role); }
    public String getPhone() { return phone; }
    public int getVisitCount() { return visitCount; }
    public boolean isVip() { return vip; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email == null ? "" : email.trim().toLowerCase(); }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
    public void setVip(boolean vip) { this.vip = vip; }
    public void recordVisit() { visitCount++; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return customerId.equals(other.customerId);
    }
}