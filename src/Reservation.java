import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Comparable<Reservation>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String reservationId;
    private final String customerId;
    private int tableId;
    private int partySize;
    private LocalDateTime dateTime;
    private int durationMinutes;
    private ReservationStatus status;

    public Reservation(String reservationId, String customerId, int tableId, int partySize, LocalDateTime dateTime, int durationMinutes) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.tableId = tableId;
        this.partySize = partySize;
        this.dateTime = dateTime;
        this.durationMinutes = durationMinutes;
        this.status = ReservationStatus.CONFIRMED;
    }

    public String getReservationId() { return reservationId; }
    public String getCustomerId() { return customerId; }
    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public LocalDateTime getEndTime() { return dateTime.plusMinutes(durationMinutes); }

    public boolean overlaps(Reservation other) {
        return tableId == other.tableId
                && dateTime.isBefore(other.getEndTime())
                && other.dateTime.isBefore(getEndTime());
    }

    @Override
    public int compareTo(Reservation other) {
        int c = dateTime.compareTo(other.dateTime);
        if (c != 0) return c;
        return reservationId.compareTo(other.reservationId);
    }
}