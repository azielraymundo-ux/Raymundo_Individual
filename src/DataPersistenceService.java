import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataPersistenceService {
    private static final Path CREDENTIALS_JSON = Paths.get("data", "credentials.json");
    private static final Path TABLES_JSON = Paths.get("data", "tables.json");
    private static final Path MENU_JSON = Paths.get("data", "menu.json");
    private static final Path STATE_SER = Paths.get("data", "system_state.ser");

    public static class SystemSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public TableManager tableManager;
        public ReservationManager reservationManager;
        public WaitlistManager waitlistManager;
        public MenuOrderManager menuOrderManager;
    }

    public static void saveCredentialsToJson(List<Customer> accounts) {
        try {
            Files.createDirectories(CREDENTIALS_JSON.getParent());
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < accounts.size(); i++) {
                Customer c = accounts.get(i);
                sb.append("  {\n")
                        .append("    \"customerId\": \"").append(escape(c.getCustomerId())).append("\",\n")
                        .append("    \"name\": \"").append(escape(c.getName())).append("\",\n")
                        .append("    \"email\": \"").append(escape(c.getEmail())).append("\",\n")
                        .append("    \"password\": \"").append(escape(c.getPassword())).append("\",\n")
                        .append("    \"role\": \"").append(escape(c.getRole())).append("\",\n")
                        .append("    \"phone\": \"").append(escape(c.getPhone())).append("\",\n")
                        .append("    \"visitCount\": ").append(c.getVisitCount()).append(",\n")
                        .append("    \"vip\": ").append(c.isVip()).append("\n")
                        .append("  }").append(i < accounts.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]\n");
            Files.writeString(CREDENTIALS_JSON, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save credentials JSON: " + e.getMessage());
        }
    }

    public static List<Customer> loadCredentialsFromJson() {
        List<Customer> list = new ArrayList<>();
        if (!Files.exists(CREDENTIALS_JSON)) {
            list.add(new Customer("MGR01", "Head Manager", "manager@plateandbook.com", "admin123", "MANAGER", "09170000000", true));
            saveCredentialsToJson(list);
            return list;
        }
        try {
            String json = Files.readString(CREDENTIALS_JSON, StandardCharsets.UTF_8);
            Pattern objPattern = Pattern.compile("\\{([^}]*)\\}");
            Matcher objMatcher = objPattern.matcher(json);
            while (objMatcher.find()) {
                String block = objMatcher.group(1);
                String id = extractString(block, "customerId");
                String name = extractString(block, "name");
                String email = extractString(block, "email");
                String password = extractString(block, "password");
                String role = extractString(block, "role");
                String phone = extractString(block, "phone");
                int visits = Integer.parseInt(extractNumber(block, "visitCount", "0"));
                boolean vip = Boolean.parseBoolean(extractNumber(block, "vip", "false"));
                if (!id.isEmpty() && !name.isEmpty()) {
                    Customer c = new Customer(id, name, email, password, role, phone, vip);
                    c.setVisitCount(visits);
                    list.add(c);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading credentials JSON: " + e.getMessage());
        }
        return list;
    }

    public static void saveSystemState(TableManager tm, ReservationManager rm, WaitlistManager wm, MenuOrderManager mom) {
        try {
            Files.createDirectories(STATE_SER.getParent());
            SystemSnapshot snap = new SystemSnapshot();
            snap.tableManager = tm;
            snap.reservationManager = rm;
            snap.waitlistManager = wm;
            snap.menuOrderManager = mom;
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_SER.toFile()))) {
                oos.writeObject(snap);
            }
        } catch (IOException e) {
            System.err.println("Failed to serialize state: " + e.getMessage());
        }
    }

    public static SystemSnapshot loadSystemState() {
        if (!Files.exists(STATE_SER)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STATE_SER.toFile()))) {
            return (SystemSnapshot) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }


    // ---------- tables.json (table count, availability, seats) ----------

    public static void saveTablesToJson(TableManager tm) {
        try {
            Files.createDirectories(TABLES_JSON.getParent());
            List<Table> tables = tm.getAllTables();
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"totalTables\": ").append(tm.getTableCount()).append(",\n");
            sb.append("  \"availableTables\": ").append(tm.getAvailableTableCount()).append(",\n");
            sb.append("  \"totalSeats\": ").append(tm.getTotalCapacity()).append(",\n");
            sb.append("  \"tables\": [\n");
            for (int i = 0; i < tables.size(); i++) {
                Table t = tables.get(i);
                sb.append("    { \"tableId\": ").append(t.getTableId())
                        .append(", \"seats\": ").append(t.getCapacity())
                        .append(", \"status\": \"").append(t.getStatus().name()).append("\" }")
                        .append(i < tables.size() - 1 ? "," : "").append("\n");
            }
            sb.append("  ]\n}\n");
            Files.writeString(TABLES_JSON, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save tables JSON: " + e.getMessage());
        }
    }

    /** Returns null if the file is missing or unreadable, so the caller keeps its current tables. */
    public static List<Table> loadTablesFromJson() {
        if (!Files.exists(TABLES_JSON)) return null;
        try {
            Object root = new MiniJson(Files.readString(TABLES_JSON, StandardCharsets.UTF_8)).parse();
            List<?> rows = root instanceof Map<?, ?> m ? (List<?>) m.get("tables") : (List<?>) root;
            List<Table> result = new ArrayList<>();
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (Object o : rows) {
                Map<?, ?> row = (Map<?, ?>) o;
                int id = ((Number) row.get("tableId")).intValue();
                int seats = ((Number) row.get("seats")).intValue();
                if (seats <= 0 || !seen.add(id)) continue;
                Table t = new Table(id, seats);
                Object st = row.get("status");
                if (st != null) {
                    try { t.setStatus(TableStatus.valueOf(st.toString())); } catch (IllegalArgumentException ignored) {}
                }
                result.add(t);
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error reading tables JSON: " + e.getMessage());
            return null;
        }
    }

    // ---------- menu.json (menu items) ----------

    public static void saveMenuToJson(MenuOrderManager mom) {
        try {
            Files.createDirectories(MENU_JSON.getParent());
            List<MenuItem> items = mom.getAllMenuItems();
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"totalItems\": ").append(items.size()).append(",\n");
            sb.append("  \"items\": [\n");
            for (int i = 0; i < items.size(); i++) {
                MenuItem m = items.get(i);
                sb.append("    {\n")
                        .append("      \"id\": \"").append(escape(m.getId())).append("\",\n")
                        .append("      \"name\": \"").append(escape(m.getName())).append("\",\n")
                        .append("      \"category\": \"").append(escape(m.getCategory())).append("\",\n")
                        .append("      \"price\": ").append(m.getPrice()).append(",\n")
                        .append("      \"description\": \"").append(escape(m.getDescription())).append("\"\n")
                        .append("    }").append(i < items.size() - 1 ? "," : "").append("\n");
            }
            sb.append("  ]\n}\n");
            Files.writeString(MENU_JSON, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save menu JSON: " + e.getMessage());
        }
    }

    /** Returns null if the file is missing or unreadable, so the caller keeps its current menu. */
    public static List<MenuItem> loadMenuFromJson() {
        if (!Files.exists(MENU_JSON)) return null;
        try {
            Object root = new MiniJson(Files.readString(MENU_JSON, StandardCharsets.UTF_8)).parse();
            List<?> rows = root instanceof Map<?, ?> m ? (List<?>) m.get("items") : (List<?>) root;
            List<MenuItem> result = new ArrayList<>();
            for (Object o : rows) {
                Map<?, ?> row = (Map<?, ?>) o;
                String id = str(row.get("id"));
                String name = str(row.get("name"));
                if (id.isBlank() || name.isBlank()) continue;
                double price = row.get("price") instanceof Number n ? n.doubleValue() : 0;
                result.add(new MenuItem(id, name, str(row.get("category")), price, str(row.get("description"))));
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error reading menu JSON: " + e.getMessage());
            return null;
        }
    }

    // ---------- shared helpers ----------

    /** Startup: JSON files (if present) override the tables/menu; otherwise they are created from the current state. */
    public static void applyJsonData(TableManager tm, MenuOrderManager mom) {
        List<Table> tables = loadTablesFromJson();
        if (tables != null) tm.replaceTables(tables); else saveTablesToJson(tm);
        List<MenuItem> menu = loadMenuFromJson();
        if (menu != null) mom.replaceMenu(menu); else saveMenuToJson(mom);
    }

    public static void saveDataJson(TableManager tm, MenuOrderManager mom) {
        saveTablesToJson(tm);
        saveMenuToJson(mom);
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    /** Minimal JSON reader (objects, arrays, strings, numbers, booleans, null) so no extra dependency is needed. */
    private static final class MiniJson {
        private final String s;
        private int i = 0;

        MiniJson(String s) { this.s = s; }

        Object parse() {
            Object v = value();
            ws();
            if (i != s.length()) throw new IllegalStateException("Unexpected trailing data at " + i);
            return v;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            ws();
            if (i >= s.length()) throw new IllegalStateException("Unexpected end of JSON");
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw new IllegalStateException("Unexpected character '" + c + "' at " + i);
            return Double.parseDouble(s.substring(start, i));
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return map; }
            while (true) {
                ws();
                String key = string();
                ws();
                if (s.charAt(i++) != ':') throw new IllegalStateException("Expected ':' at " + (i - 1));
                map.put(key, value());
                ws();
                char c = s.charAt(i++);
                if (c == '}') return map;
                if (c != ',') throw new IllegalStateException("Expected ',' or '}' at " + (i - 1));
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return list; }
            while (true) {
                list.add(value());
                ws();
                char c = s.charAt(i++);
                if (c == ']') return list;
                if (c != ',') throw new IllegalStateException("Expected ',' or ']' at " + (i - 1));
            }
        }

        private String string() {
            if (s.charAt(i) != '"') throw new IllegalStateException("Expected string at " + i);
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String extractString(String block, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(block);
        return m.find() ? m.group(1) : "";
    }

    private static String extractNumber(String block, String key, String def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*([a-zA-Z0-9]+)").matcher(block);
        return m.find() ? m.group(1) : def;
    }
}