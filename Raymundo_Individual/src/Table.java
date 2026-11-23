import java.io.Serializable;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int tableId;
    private final int capacity;
    private TableStatus status;

    public Table(int tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

    public int getTableId() {
        return tableId;
    }

    public int getCapacity() {
        return capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public void setStatus(TableStatus status) {
        this.status = status;
    }

    public boolean seatsAvail(int partySize) {
        return partySize > 0 && partySize <= capacity;
    }
}