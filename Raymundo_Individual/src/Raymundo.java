import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Raymundo's parts uses:
 *   1. WaitQueue            enqueue, dequeue, isEmpty       (Start Day reservation line in Main.java)
 *   2. PriorityWaitQueue    enqueue (VIP auto-priority), remove, getAll, size
 *   3. WaitlistManager      addToWaitlist, getWaitlist, seatNextEligibleGuest, cancelWaitlist, getWaitlistSize
 *   4. Receipt billing      PWD/Senior 20%, 12% VAT, 10% service charge
 *   5. Reservations         customer HashMap lookup, opening hours, double booking, cancel rule
 */

public class Raymundo {

    private static final Scanner in = new Scanner(System.in);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final WaitQueue<String> fifo = new WaitQueue<>();
    private static final PriorityWaitQueue pq = new PriorityWaitQueue();
    private static final WaitlistManager waitlist = new WaitlistManager();
    private static final ReservationManager reservations = new ReservationManager();
    private static int pqCounter = 1;

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n========== QUEUE & SERVICE TESTER ==========");
            System.out.println("1 - WaitQueue (FIFO)");
            System.out.println("2 - PriorityWaitQueue (VIP first)");
            System.out.println("3 - WaitlistManager (walk-ins and seating)");
            System.out.println("4 - Receipt / billing calculator");
            System.out.println("5 - Reservations (customer lookup, hours, double booking, cancel)");
            System.out.println("0 - Exit");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> waitQueueMenu();
                case 2 -> priorityMenu();
                case 3 -> waitlistMenu();
                case 4 -> billingMenu();
                case 5 -> reservationMenu();
                case 0 -> { System.out.println("Bye."); return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void waitQueueMenu() {
        while (true) {
            System.out.println("\n--- WaitQueue (first in, first out) ---");
            System.out.println("1 - enqueue  2 - dequeue  3 - isEmpty  0 - back");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> {
                    String item = readLine("Item to add (e.g. RES001): ");
                    if (item.isEmpty()) { System.out.println("Nothing added."); break; }
                    fifo.enqueue(item);
                    System.out.println("Added '" + item + "'.");
                }
                case 2 -> {
                    String out = fifo.dequeue();
                    System.out.println(out == null ? "Queue is empty (dequeue returned null)." : "Dequeued: " + out);
                }
                case 3 -> System.out.println("isEmpty = " + fifo.isEmpty());
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void priorityMenu() {
        while (true) {
            System.out.println("\n--- PriorityWaitQueue (VIP = priority 10, regular = priority 1) ---");
            System.out.println("1 - add guest (VIP y/n)  2 - remove by ID  3 - show all  4 - size  0 - back");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> {
                    String name = readLine("Guest name: ");
                    if (name.isEmpty()) name = "Guest" + pqCounter;
                    int party = Math.max(1, readInt("Party size: "));
                    boolean vip = readYesNo("VIP? (y/n): ");
                    WalkinGuest g = new WalkinGuest(String.format("T%03d", pqCounter++), name, "", party, vip, LocalDateTime.now());
                    pq.enqueue(g);
                    System.out.println("Added " + describe(g) + ".");
                }
                case 2 -> {
                    String id = readLine("Guest ID to remove (e.g. T001): ");
                    System.out.println(pq.remove(id) ? "Removed " + id + "." : "No guest with ID " + id + ".");
                }
                case 3 -> printGuests(pq.getAll());
                case 4 -> System.out.println("size = " + pq.size());
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void waitlistMenu() {
        while (true) {
            System.out.println("\n--- WaitlistManager ---");
            System.out.println("1 - add walk-in  2 - show waitlist  3 - seat next eligible guest at a table");
            System.out.println("4 - cancel by ID  5 - waitlist size  0 - back");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> {
                    String name = readLine("Guest name: ");
                    int party = Math.max(1, readInt("Party size: "));
                    boolean vip = readYesNo("VIP? (y/n): ");
                    WalkinGuest g = waitlist.addToWaitlist(name, "", party, vip);
                    System.out.println("Added " + describe(g) + ".");
                }
                case 2 -> printGuests(waitlist.getWaitlist());
                case 3 -> {
                    int seats = readInt("How many seats does the free table have? ");
                    Table t = new Table(99, seats);
                    WalkinGuest g = waitlist.seatNextEligibleGuest(t);
                    if (g == null) System.out.println("Nobody waiting fits a " + seats + "-seat table.");
                    else System.out.println("Seated " + describe(g) + ". Table status is now " + t.getStatus() + ".");
                }
                case 4 -> {
                    String id = readLine("Guest ID to cancel (e.g. WL001): ");
                    System.out.println(waitlist.cancelWaitlist(id) ? "Removed " + id + "." : "No guest with ID " + id + ".");
                }
                case 5 -> System.out.println("waitlist size = " + waitlist.getWaitlistSize());
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void billingMenu() {
        System.out.println("\n--- Receipt / billing calculator ---");
        double food = readDouble("Food total in PHP (VAT-inclusive menu prices, e.g. 1000): ");
        int party = Math.max(1, readInt("Total diners in party: "));
        int pwd = Math.max(0, Math.min(readInt("How many are PWD / Senior Citizen: "), party));

        MenuItem item = new MenuItem("TEST", "Test order", "Main Course", food, "");
        Order order = new Order("ORD-TEST", "RES-TEST", List.of(new CartItem(item, 1, "")));
        System.out.println(ReportService.formatReceipt(order, null, null, party, pwd, "CASH", null));
    }

    private static void reservationMenu() {
        while (true) {
            System.out.println("\n--- Reservations (open 10:00-22:00, cancel at least 8 hours ahead) ---");
            System.out.println("1 - add reservation  2 - list by customer ID (HashMap lookup)  3 - list all");
            System.out.println("4 - cancel reservation (as customer)  0 - back");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> {
                    String cust = readLine("Customer ID (e.g. C001): ");
                    int table = readInt("Table number: ");
                    int party = readInt("Party size: ");
                    LocalDateTime when = readDateTime("Date and time (yyyy-MM-dd HH:mm): ");
                    if (when == null) break;
                    int dur = readInt("Duration in minutes: ");
                    try {
                        Reservation r = reservations.createReservation(cust, table, party, when, dur, "");
                        System.out.println("Booked " + r.getReservationId() + ".");
                    } catch (RuntimeException e) {
                        System.out.println("Rejected: " + e.getMessage());
                    }
                }
                case 2 -> {
                    String cust = readLine("Customer ID: ");
                    List<Reservation> list = reservations.getReservationsForCustomer(cust);
                    if (list.isEmpty()) System.out.println("No reservations for " + cust + ".");
                    for (Reservation r : list) System.out.println("  " + describe(r));
                }
                case 3 -> {
                    List<Reservation> all = reservations.getAllReservations();
                    if (all.isEmpty()) System.out.println("No reservations yet.");
                    for (Reservation r : all) System.out.println("  " + describe(r));
                }
                case 4 -> {
                    String id = readLine("Reservation ID (e.g. RES001): ").toUpperCase();
                    try {
                        boolean ok = reservations.cancelReservation(id, LocalDateTime.now(), false);
                        System.out.println(ok ? "Cancelled " + id + "." : "Could not cancel " + id + " (not found or not CONFIRMED).");
                    } catch (RuntimeException e) {
                        System.out.println("Rejected: " + e.getMessage());
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
    }

    private static boolean throwsException(Runnable r) {
        try { r.run(); return false; } catch (RuntimeException e) { return true; }
    }

    private static WalkinGuest guest(String id, String name, int party, boolean vip) {
        return new WalkinGuest(id, name, "", party, vip, LocalDateTime.now());
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    private static String describe(WalkinGuest g) {
        return g.getGuestId() + " " + g.getGuestName() + " (party of " + g.getPartySize() + (g.isVip() ? ", VIP" : "") + ")";
    }

    private static String describe(Reservation r) {
        return r.getReservationId() + " | customer " + r.getCustomerId() + " | table " + r.getTableId()
                + " | party " + r.getPartySize() + " | " + r.getDateTime().format(DT) + " | " + r.getStatus();
    }

    private static void printGuests(List<WalkinGuest> list) {
        if (list.isEmpty()) {
            System.out.println("(empty)");
            return;
        }
        System.out.println("Next to be served is at the top:");
        int i = 1;
        for (WalkinGuest g : list) System.out.println("  " + i++ + ". " + describe(g));
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) return 0;
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please type a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) return 0;
            try {
                return Double.parseDouble(in.nextLine().trim().replace(",", ""));
            } catch (NumberFormatException e) {
                System.out.println("Please type a number.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        return readLine(prompt).equalsIgnoreCase("y");
    }

    private static LocalDateTime readDateTime(String prompt) {
        String s = readLine(prompt);
        try {
            return LocalDateTime.parse(s, DT);
        } catch (DateTimeParseException e) {
            System.out.println("Use the format yyyy-MM-dd HH:mm, for example 2026-12-01 18:30.");
            return null;
        }
    }
}