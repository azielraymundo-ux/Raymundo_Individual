import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class ReservationManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    public static final long MIN_CANCEL_HOURS_AHEAD = 8;

    private final ReservationBST bst = new ReservationBST();
    private final Map<String, String> specialRequests = new HashMap<>();
    private transient Map<String, List<Reservation>> reservationsByCustomer = new HashMap<>();
    private int reservationCounter = 1;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        rebuildCustomerIndex();
    }

    public void rebuildCustomerIndex() {
        if (reservationsByCustomer == null) {
            reservationsByCustomer = new HashMap<>();
        } else {
            reservationsByCustomer.clear();
        }
        for (Reservation r : bst.inOrder()) {
            reservationsByCustomer
                    .computeIfAbsent(r.getCustomerId(), k -> new ArrayList<>())
                    .add(r);
        }
    }

    private Map<String, List<Reservation>> customerMap() {
        if (reservationsByCustomer == null) {
            rebuildCustomerIndex();
        }
        return reservationsByCustomer;
    }

    public void validateOpeningHours(LocalDateTime start, int durationMinutes) {
        LocalTime startTime = start.toLocalTime();
        LocalDateTime end = start.plusMinutes(durationMinutes);
        if (startTime.isBefore(OPENING_TIME)
                || !end.toLocalDate().equals(start.toLocalDate())
                || end.toLocalTime().isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException(
                    "Outside opening hours (" + OPENING_TIME + " - " + CLOSING_TIME + ").");
        }
    }

    public Reservation createReservation(String customerId, int tableId, int partySize,
                                         LocalDateTime dateTime, int durationMinutes, String specialRequest) {
        validateOpeningHours(dateTime, durationMinutes);

        String id = String.format("RES%03d", reservationCounter++);
        Reservation reservation = new Reservation(id, customerId, tableId, partySize, dateTime, durationMinutes);

        Reservation conflict = bst.findConflict(reservation);
        if (conflict != null) {
            throw new IllegalStateException("Table " + tableId + " is blocked from "
                    + conflict.getDateTime() + " to " + conflict.getEndTime());
        }

        bst.insert(reservation);
        customerMap().computeIfAbsent(customerId, k -> new ArrayList<>()).add(reservation);
        if (specialRequest != null && !specialRequest.isBlank()) {
            specialRequests.put(id, specialRequest.trim());
        }
        return reservation;
    }

    public Reservation editReservation(String reservationId, int newTableId, int newPartySize,
                                       LocalDateTime newDateTime, int newDuration, String newSpecialRequest) {
        Reservation existing = findById(reservationId);
        if (existing == null || existing.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed reservations can be edited.");
        }
        validateOpeningHours(newDateTime, newDuration);

        Reservation probe = new Reservation(reservationId, existing.getCustomerId(),
                newTableId, newPartySize, newDateTime, newDuration);
        Reservation conflict = bst.findConflict(probe);
        if (conflict != null) {
            throw new IllegalStateException("Table " + newTableId + " is already blocked during that time.");
        }

        bst.remove(existing);
        existing.setTableId(newTableId);
        existing.setPartySize(newPartySize);
        existing.setDateTime(newDateTime);
        existing.setDurationMinutes(newDuration);
        bst.insert(existing);

        if (newSpecialRequest != null) {
            if (newSpecialRequest.isBlank()) specialRequests.remove(reservationId);
            else specialRequests.put(reservationId, newSpecialRequest.trim());
        }
        return existing;
    }

    public boolean cancelReservation(String reservationId, LocalDateTime now, boolean managerOverride) {
        Reservation r = findById(reservationId);
        if (r == null || r.getStatus() != ReservationStatus.CONFIRMED) return false;

        long hoursUntil = Duration.between(now, r.getDateTime()).toHours();
        if (!managerOverride && hoursUntil < MIN_CANCEL_HOURS_AHEAD) {
            throw new IllegalStateException("Cancellations must be made at least "
                    + MIN_CANCEL_HOURS_AHEAD + " hours ahead of reservation time.");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        return bst.remove(r);
    }

    public List<Reservation> getReservationsForCustomer(String customerId) {
        if (customerId == null) return Collections.emptyList();
        return new ArrayList<>(customerMap().getOrDefault(customerId, Collections.emptyList()));
    }

    public String getSpecialRequest(String reservationId) { return specialRequests.getOrDefault(reservationId, "None"); }
    public Reservation findById(String reservationId) { return bst.findById(reservationId); }
    public List<Reservation> getAllReservations() { return bst.inOrder(); }

    public List<Reservation> getScheduleForDate(LocalDate date) {
        return bst.getBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public boolean checkInReservation(String reservationId, TableManager tableManager) {
        Reservation r = findById(reservationId);
        if (r != null && r.getStatus() == ReservationStatus.CONFIRMED) {
            r.setStatus(ReservationStatus.SEATED);
            if (tableManager != null) tableManager.updateTableStatus(r.getTableId(), TableStatus.OCCUPIED);
            return true;
        }
        return false;
    }

    public boolean completeReservation(String reservationId, TableManager tableManager, CustomerManager customerManager) {
        Reservation r = findById(reservationId);
        if (r != null && r.getStatus() == ReservationStatus.SEATED) {
            r.setStatus(ReservationStatus.COMPLETED);
            if (tableManager != null) tableManager.updateTableStatus(r.getTableId(), TableStatus.AVAILABLE);
            if (customerManager != null) customerManager.recordCustomerVisit(r.getCustomerId());
            return true;
        }
        return false;
    }
}