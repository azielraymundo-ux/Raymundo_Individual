import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TableManager implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_TWO_SEATER_TABLES = 10;

    private final List<Table> tables = new ArrayList<>();

    public void addTable(Table table) {
        if (table == null) throw new IllegalArgumentException("Table cannot be null");
        for (Table t : tables) {
            if (t.getTableId() == table.getTableId()) {
                throw new IllegalArgumentException("Table number " + table.getTableId() + " already exists.");
            }
        }
        if (table.getCapacity() == 2 && getTwoSeaterCount() >= MAX_TWO_SEATER_TABLES) {
            throw new IllegalStateException("Maximum number of tables for 2 (" + MAX_TWO_SEATER_TABLES + ") reached.");
        }
        tables.add(table);
    }

    public void addTable(int tableId, int capacity) {
        addTable(new Table(tableId, capacity));
    }

    public void editTable(int currentTableId, int newTableId, int newCapacity, List<Reservation> allReservations) {
        Table existing = findById(currentTableId);
        if (existing == null) {
            throw new IllegalArgumentException("Table " + currentTableId + " does not exist.");
        }
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }
        if (currentTableId != newTableId && findById(newTableId) != null) {
            throw new IllegalArgumentException("Table number " + newTableId + " already exists.");
        }

        int projectedTwoSeaters = getTwoSeaterCount()
                - (existing.getCapacity() == 2 ? 1 : 0)
                + (newCapacity == 2 ? 1 : 0);
        if (projectedTwoSeaters > MAX_TWO_SEATER_TABLES) {
            throw new IllegalStateException("Maximum number of tables for 2 (" + MAX_TWO_SEATER_TABLES + ") reached.");
        }

        if (allReservations != null) {
            for (Reservation r : allReservations) {
                if (r.getTableId() == currentTableId
                        && (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.SEATED)
                        && r.getPartySize() > newCapacity) {
                    throw new IllegalStateException("Active/confirmed reservation " + r.getReservationId()
                            + " requires " + r.getPartySize() + " seats.");
                }
            }
        }

        TableStatus prevStatus = existing.getStatus();
        int idx = tables.indexOf(existing);
        Table replacement = new Table(newTableId, newCapacity);
        replacement.setStatus(prevStatus);
        tables.set(idx, replacement);
    }

    public void deleteTable(int tableId, List<Reservation> allReservations) {
        Table existing = findById(tableId);
        if (existing == null) {
            throw new IllegalArgumentException("Table " + tableId + " does not exist.");
        }
        if (existing.getStatus() == TableStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot delete Table " + tableId + " while it is OCCUPIED.");
        }
        if (allReservations != null) {
            for (Reservation r : allReservations) {
                if (r.getTableId() == tableId
                        && (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.SEATED)) {
                    throw new IllegalStateException("Cannot delete Table " + tableId + " with active/confirmed reservation "
                            + r.getReservationId() + ".");
                }
            }
        }
        tables.remove(existing);
    }

    public int getTwoSeaterCount() {
        int count = 0;
        for (Table t : tables) {
            if (t.getCapacity() == 2) count++;
        }
        return count;
    }

    public Table findById(int tableId) {
        for (Table t : tables) {
            if (t.getTableId() == tableId) return t;
        }
        return null;
    }

    public List<Table> getAllTables() { return new ArrayList<>(tables); }

    public List<Table> getAvailableTables() {
        List<Table> available = new ArrayList<>();
        for (Table t : tables) {
            if (t.getStatus() == TableStatus.AVAILABLE) available.add(t);
        }
        return available;
    }

    public TableStatus getDisplayStatus(Table table, LocalDate referenceDate, List<Reservation> allReservations) {
        if (table.getStatus() == TableStatus.OCCUPIED) return TableStatus.OCCUPIED;

        boolean hasSameDay = false;
        boolean hasFutureDay = false;
        for (Reservation r : allReservations) {
            if (r.getTableId() == table.getTableId() && r.getStatus() == ReservationStatus.CONFIRMED) {
                LocalDate resDate = r.getDateTime().toLocalDate();
                if (resDate.equals(referenceDate)) {
                    hasSameDay = true;
                } else if (resDate.isAfter(referenceDate)) {
                    hasFutureDay = true;
                }
            }
        }
        if (hasSameDay) return TableStatus.RESERVED;
        if (hasFutureDay) return TableStatus.UPCOMING;
        return TableStatus.AVAILABLE;
    }

    public boolean updateTableStatus(int tableId, TableStatus status) {
        Table t = findById(tableId);
        if (t != null) {
            t.setStatus(status);
            return true;
        }
        return false;
    }

    public int getTotalCapacity() {
        return tables.stream().mapToInt(Table::getCapacity).sum();
    }

    public void replaceTables(List<Table> newTables) {
        tables.clear();
        if (newTables != null) tables.addAll(newTables);
    }

    public int getAvailableTableCount() { return getAvailableTables().size(); }

    public int getTableCount() { return tables.size(); }
}