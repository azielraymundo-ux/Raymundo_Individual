import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class WaitlistManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PriorityWaitQueue waitQueue;
    private int guestCounter;

    public WaitlistManager() {
        this.waitQueue = new PriorityWaitQueue();
        this.guestCounter = 1;
    }

    public WalkinGuest addToWaitlist(String customerName, String phone, int partySize, boolean vip) {
        String guestId = String.format("WL%03d", guestCounter++);
        WalkinGuest guest = new WalkinGuest(guestId, customerName, phone, partySize, vip, LocalDateTime.now());
        waitQueue.enqueue(guest);
        return guest;
    }

    public void addToWaitlist(WalkinGuest guest) { waitQueue.enqueue(guest); }
    public WalkinGuest getNextGuest() { return waitQueue.dequeue(); }
    public WalkinGuest peekNextGuest() { return waitQueue.peek(); }
    public boolean cancelWaitlist(String guestId) { return waitQueue.remove(guestId); }
    public List<WalkinGuest> getWaitlist() { return waitQueue.getAll(); }
    public int getWaitlistSize() { return waitQueue.size(); }
    public boolean hasWaitingGuests() { return !waitQueue.isEmpty(); }

    public WalkinGuest seatNextEligibleGuest(Table table) {
        if (table == null || table.getStatus() != TableStatus.AVAILABLE) return null;

        List<WalkinGuest> allGuests = waitQueue.getAll();
        for (WalkinGuest guest : allGuests) {
            if (table.seatsAvail(guest.getPartySize())) {
                waitQueue.remove(guest.getGuestId());
                table.setStatus(TableStatus.OCCUPIED);
                return guest;
            }
        }
        return null;
    }

    public int estimateWaitTimeMinutes(String guestId) {
        List<WalkinGuest> allGuests = waitQueue.getAll();
        int position = 0;
        for (WalkinGuest guest : allGuests) {
            if (guest.getGuestId().equals(guestId)) return position * 15;
            position++;
        }
        return -1;
    }
}