import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter REPORT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double VAT_RATE = 0.12;
    private static final double PWD_SENIOR_DISCOUNT_RATE = 0.20;
    private static final double SERVICE_CHARGE_RATE = 0.10;

    public static class BillingBreakdown {
        public final int partySize;
        public final int qualifiedPwdSeniorCount;
        public final double grossMenuSubtotal;
        public final double regularShareGross;
        public final double qualifiedShareGross;
        public final double vatExemptAmount;
        public final double pwdSeniorDiscount;
        public final double netAfterDiscount;
        public final double vatAmount;
        public final double serviceCharge;
        public final double totalDue;
        public final String paymentMethod;
        public final double amountPaid;
        public final double changeDue;

        public BillingBreakdown(int partySize, int qualifiedPwdSeniorCount, double grossMenuSubtotal,
                                double regularShareGross, double qualifiedShareGross, double vatExemptAmount,
                                double pwdSeniorDiscount, double netAfterDiscount, double vatAmount,
                                double serviceCharge, double totalDue, String paymentMethod,
                                double amountPaid, double changeDue) {
            this.partySize = partySize;
            this.qualifiedPwdSeniorCount = qualifiedPwdSeniorCount;
            this.grossMenuSubtotal = grossMenuSubtotal;
            this.regularShareGross = regularShareGross;
            this.qualifiedShareGross = qualifiedShareGross;
            this.vatExemptAmount = vatExemptAmount;
            this.pwdSeniorDiscount = pwdSeniorDiscount;
            this.netAfterDiscount = netAfterDiscount;
            this.vatAmount = vatAmount;
            this.serviceCharge = serviceCharge;
            this.totalDue = totalDue;
            this.paymentMethod = paymentMethod == null || paymentMethod.isBlank() ? "CASH" : paymentMethod.trim().toUpperCase();
            this.amountPaid = amountPaid;
            this.changeDue = changeDue;
        }
    }

    public static BillingBreakdown computeBilling(Order order, int partySize, int qualifiedPwdSeniorCount,
                                                  String paymentMethod, Double amountPaidOverride) {
        int safeParty = Math.max(1, partySize);
        int safeQualified = Math.max(0, Math.min(qualifiedPwdSeniorCount, safeParty));

        double grossSubtotal = order.getTotalAmount();
        double qualifiedShareGross = grossSubtotal * ((double) safeQualified / safeParty);
        double regularShareGross = grossSubtotal - qualifiedShareGross;

        // Standard PH rules: PWD/Senior share is VAT-exempt (gross / 1.12), then receives 20% discount.
        double qualifiedNetOfVat = qualifiedShareGross / (1.0 + VAT_RATE);
        double vatExemptAmount = qualifiedShareGross - qualifiedNetOfVat;
        double pwdSeniorDiscount = qualifiedNetOfVat * PWD_SENIOR_DISCOUNT_RATE;
        double qualifiedPayable = qualifiedNetOfVat - pwdSeniorDiscount;

        // Regular diner share retains standard 12% VAT breakdown.
        double regularVatableSales = regularShareGross / (1.0 + VAT_RATE);
        double vatAmount = regularShareGross - regularVatableSales;

        double netAfterDiscount = regularShareGross + qualifiedPayable;
        double serviceCharge = netAfterDiscount * SERVICE_CHARGE_RATE;
        double totalDue = netAfterDiscount + serviceCharge;

        double paid = (amountPaidOverride != null && amountPaidOverride >= totalDue) ? amountPaidOverride : totalDue;
        double change = Math.max(0.0, paid - totalDue);

        return new BillingBreakdown(safeParty, safeQualified, grossSubtotal, regularShareGross,
                qualifiedShareGross, vatExemptAmount, pwdSeniorDiscount, netAfterDiscount,
                vatAmount, serviceCharge, totalDue, paymentMethod, paid, change);
    }

    public static String formatReceipt(Order order, Customer customer, Reservation reservation) {
        int party = (reservation != null) ? reservation.getPartySize() : 1;
        return formatReceipt(order, customer, reservation, party, 0, "CASH", null);
    }

    public static String formatReceipt(Order order, Customer customer, Reservation reservation,
                                       int partySize, int qualifiedPwdSeniorCount,
                                       String paymentMethod, Double amountPaidOverride) {
        BillingBreakdown b = computeBilling(order, partySize, qualifiedPwdSeniorCount, paymentMethod, amountPaidOverride);

        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("                PLATE & BOOK                  \n");
        sb.append("          2401 TAFT AVE, MANILA 1004          \n");
        sb.append("             (02) 8524-4611                 \n");
        sb.append("         HELLO@PLATEANDBOOK.COM.PH           \n");
        sb.append("==============================================\n");
        sb.append("              ").append(order.getOrderTime().format(TS)).append("\n");
        sb.append(String.format("ORDER # : %-14s GUEST : %-12s%n",
                order.getOrderId(), customer != null ? customer.getName() : "WALK-IN"));
        sb.append(String.format("REF ID  : %-14s TABLE : %-12s%n",
                order.getReferenceId(), reservation != null ? "TBL-" + reservation.getTableId() : "N/A"));
        sb.append(String.format("PARTY   : %-14d PWD/SC: %-12d%n",
                b.partySize, b.qualifiedPwdSeniorCount));
        sb.append("----------------------------------------------\n");

        for (CartItem ci : order.getItems()) {
            sb.append(String.format("%dX %s (%s)%n",
                    ci.getQuantity(), ci.getMenuItem().getName().toUpperCase(), ci.getMenuItem().getId()));
            sb.append(String.format("%46s%n", String.format("PHP %.2f", ci.getTotalPrice())));
            if (ci.getSpecialRequests() != null && !ci.getSpecialRequests().isBlank()) {
                sb.append("  NOTE: ").append(ci.getSpecialRequests()).append("\n");
            }
        }

        sb.append("==============================================\n");
        sb.append(String.format("%-26s PHP %12.2f%n", "GROSS SUBTOTAL:", b.grossMenuSubtotal));
        if (b.qualifiedPwdSeniorCount > 0) {
            sb.append(String.format("%-26s-PHP %12.2f%n", "VAT EXEMPT (PWD/SC):", b.vatExemptAmount));
            sb.append(String.format("%-26s-PHP %12.2f%n", "PWD/SC 20% DISC:", b.pwdSeniorDiscount));
        }
        sb.append(String.format("%-26s PHP %12.2f%n", "NET SALES:", b.netAfterDiscount));
        sb.append(String.format("%-26s PHP %12.2f%n", "VAT (12%):", b.vatAmount));
        sb.append(String.format("%-26s PHP %12.2f%n", "SERVICE CHARGE (10%):", b.serviceCharge));
        sb.append(String.format("%-26s PHP %12.2f%n", "TOTAL DUE:", b.totalDue));
        sb.append("----------------------------------------------\n");
        sb.append(String.format("%-26s %16s%n", "PAYMENT METHOD:", b.paymentMethod));
        sb.append(String.format("%-26s PHP %12.2f%n", "AMOUNT PAID:", b.amountPaid));
        sb.append(String.format("%-26s PHP %12.2f%n", "CHANGE:", b.changeDue));
        sb.append("==============================================\n");
        sb.append("          THANK YOU FOR DINING WITH US!       \n");
        sb.append("    PWD/SENIOR DISCOUNTS PER RA 10754 / 9994    \n");
        sb.append("   ||||| || |||| ||| || ||||| ||| || |||| ||  \n");
        sb.append("        ").append(order.getOrderId()).append("-").append(order.getReferenceId()).append("\n");
        sb.append("==============================================\n");
        return sb.toString();
    }

    public static String generateManagerReport(TableManager tm, ReservationManager rm,
                                               CustomerManager cm, MenuOrderManager mom, WaitlistManager wm) {
        List<Reservation> allRes = rm.getAllReservations();
        List<Order> allOrders = mom.getOrderHistory();
        double grossRevenue = allOrders.stream().mapToDouble(Order::getTotalAmount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("===== PLATE & BOOK OPERATIONAL REPORT =====\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(REPORT_TS)).append("\n\n");
        sb.append("[TABLES]\n");
        sb.append("Total Tables: ").append(tm.getTableCount())
                .append(" | Total Seats: ").append(tm.getTotalCapacity())
                .append(" | 2-Seater Count: ").append(tm.getTwoSeaterCount())
                .append("/").append(TableManager.MAX_TWO_SEATER_TABLES).append("\n\n");
        sb.append("[RESERVATIONS]\n");
        sb.append("Active/Scheduled Count: ").append(allRes.size()).append("\n");
        for (Reservation r : allRes) {
            sb.append(" - ").append(r.getReservationId()).append(" | Table ").append(r.getTableId())
                    .append(" | ").append(r.getDateTime().format(REPORT_TS)).append(" | ").append(r.getStatus()).append("\n");
        }
        sb.append("\n[ORDERS & REVENUE]\n");
        sb.append("Total Orders: ").append(allOrders.size())
                .append(String.format(" | Gross Sales: PHP %.2f | With 12%% VAT + 10%% SC: PHP %.2f%n",
                        grossRevenue, grossRevenue * (1.0 + VAT_RATE + SERVICE_CHARGE_RATE)));
        sb.append("\n[CUSTOMERS & WAITLIST]\n");
        sb.append("Registered Accounts: ").append(cm.getAllCustomers().size())
                .append(" | Current Walk-in Waitlist: ").append(wm.getWaitlistSize()).append("\n");
        return sb.toString();
    }

    public static Path exportReportToFile(String content) throws IOException {
        Path dir = Paths.get("reports");
        Files.createDirectories(dir);
        Path out = dir.resolve("report_" + LocalDate.now() + ".txt");
        Files.writeString(out, content, StandardCharsets.UTF_8);
        return out;
    }
}