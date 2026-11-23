import java.io.Serializable;
import java.time.LocalDateTime;

public class WalkinGuest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String guestId;
    private String guestName;
    private String phone;
    private int partySize;
    private boolean vip;
    private LocalDateTime arrivalTime;

    public WalkinGuest(String guestId, String guestName, String phone, int partySize, boolean vip, LocalDateTime arrivalTime) {
        this.guestId = guestId;
        this.guestName = guestName;
        this.phone = phone;
        this.partySize = partySize;
        this.vip = vip;
        this.arrivalTime = arrivalTime;
    }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }
    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    @Override
    public String toString() {
        return "WalkinGuest{" +
                "guestId='" + guestId + '\'' +
                ", guestName='" + guestName + '\'' +
                ", phone='" + phone + '\'' +
                ", partySize=" + partySize +
                ", vip=" + vip +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}