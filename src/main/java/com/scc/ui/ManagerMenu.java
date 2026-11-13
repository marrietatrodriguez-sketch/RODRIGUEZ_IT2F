package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.BorrowingDao;
import com.scc.dao.EquipmentDao;
import com.scc.dao.PenaltyDao;
import com.scc.dao.ReportDao;
import com.scc.model.BorrowingTransaction;
import com.scc.model.Equipment;
import com.scc.model.Penalty;
import com.scc.model.User;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.scc.ui.MenuUtil.*;

public class ManagerMenu {

    /** Late-return fee charged per day past the due date (Penalty_Type LATE). */
    private static final double LATE_FEE_PER_DAY = 50.0;

    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final BorrowingDao borrowingDao = new BorrowingDao();
    private final PenaltyDao penaltyDao = new PenaltyDao();
    private final ReportDao reportDao = new ReportDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private final Scanner sc;
    private final User manager;

    public ManagerMenu(Scanner sc, User manager) {
        this.sc = sc;
        this.manager = manager;
    }

    /** Numeric prompt that re-asks on letters or symbols instead of crashing. */
    private int readIntInput(String prompt) {
        return readInt(sc, prompt);
    }

    public void start() {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("           MANAGER DASHBOARD");
            System.out.println("========================================");
            System.out.println("Welcome, " + manager.username());
            System.out.println("1. Manage Equipment Inventory");
            System.out.println("2. Review Borrowing Requests (approve / reject)");
            System.out.println("3. Record Equipment Return & Automatic Penalties");
            System.out.println("4. Manage Penalties (update payment status)");
            System.out.println("5. View Equipment & Borrowing Report");
            System.out.println("6. View System Audit Logs");
            System.out.println("7. Logout");
            System.out.println("========================================");

            switch (readLine(sc, "Enter your choice: ")) {
                case "1" -> manageEquipment();
                case "2" -> reviewRequests();
                case "3" -> recordReturn();
                case "4" -> managePenalties();
                case "5" -> viewReport();
                case "6" -> printLogs(activityLogDao.getAllLogs());
                case "7" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------- EQUIPMENT ----------

    private void manageEquipment() {
        while (true) {
            System.out.println("\n================ EQUIPMENT INVENTORY ================");
            System.out.println("1. View Equipment List");
            System.out.println("2. Search Equipment (name / category / condition / quantity)");
            System.out.println("3. Add Equipment");
            System.out.println("4. Update Equipment");
            System.out.println("5. Delete Equipment");
            System.out.println("6. Back");
            System.out.println("=====================================================");

            switch (readLine(sc, "Enter choice: ")) {
                case "1" -> displayEquipment(equipmentDao.getAllEquipment());
                case "2" -> displayEquipment(equipmentDao.searchEquipment(
                        readLine(sc, "Search keyword: ")));
                case "3" -> addEquipment();
                case "4" -> updateEquipment();
                case "5" -> deleteEquipment();
                case "6" -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void displayEquipment(List<Equipment> list) {
        System.out.println("\n======================= EQUIPMENT RECORDS =======================");

        if (list.isEmpty()) {
            System.out.println("No equipment records found.");
            return;
        }

        System.out.printf("%-5s | %-18s | %-12s | %-6s | %-6s | %-10s | %-10s%n",
                "ID", "Name", "Category", "Qty", "Avail", "Condition", "Cost");
        line(85);

        for (Equipment eq : list) {
            System.out.printf("%-5d | %-18s | %-12s | %-6d | %-6d | %-10s | P%-9.2f%n",
                    eq.equipmentId(), shorten(eq.name(), 18), shorten(eq.category(), 12),
                    eq.quantity(), eq.availableQuantity(), shorten(eq.condition(), 10),
                    eq.replacementCost());
        }
    }

    private void addEquipment() {
        String name = readLine(sc, "Equipment name: ");
        String category = readLine(sc, "Category: ");
        int qty = readIntInput("Quantity: ");
        String condition = readLine(sc, "Condition (e.g. Good, Fair): ");
        double cost = readDouble(sc, "Replacement cost (P): ");

        if (name.isBlank() || category.isBlank() || condition.isBlank()) {
            System.out.println("Name, category and condition are required.");
            return;
        }

        System.out.println(equipmentDao.addEquipment(name, category, qty, condition, cost, manager.userId())
                ? "Equipment added."
                : "Failed to add equipment.");
    }

    private void updateEquipment() {
        displayEquipment(equipmentDao.getAllEquipment());

        int id = readIntInput("Equipment ID to update (0 to cancel): ");

        if (id == 0) {
            return;
        }

        if (equipmentDao.findById(id) == null) {
            System.out.println("No equipment found with ID " + id + ".");
            return;
        }

        int qty = readIntInput("New total quantity: ");
        int avail = readIntInput("New available quantity: ");

        if (avail > qty) {
            System.out.println("Available quantity cannot be more than the total quantity.");
            return;
        }

        String condition = readLine(sc, "New condition: ");
        double cost = readDouble(sc, "New replacement cost (P): ");

        System.out.println(equipmentDao.updateEquipment(id, qty, avail, condition, cost, manager.userId())
                ? "Equipment updated."
                : "Update failed.");
    }

    private void deleteEquipment() {
        displayEquipment(equipmentDao.getAllEquipment());

        int id = readIntInput("Equipment ID to delete (0 to cancel): ");

        if (id == 0) {
            return;
        }

        System.out.println(equipmentDao.deleteEquipment(id, manager.userId())
                ? "Equipment deleted."
                : "Delete blocked: the ID does not exist, or the item is on a pending/approved borrowing.");
    }

    // ---------- BORROWING REQUESTS ----------

    private void reviewRequests() {
        List<BorrowingTransaction> pending = borrowingDao.getPendingRequests();

        if (pending.isEmpty()) {
            System.out.println("No pending borrowing requests.");
            return;
        }

        printTransactions(pending);

        int id = readIntInput("Request ID to act on (0 to cancel): ");

        if (id == 0) {
            return;
        }

        String decision = readLine(sc, "Enter APPROVED or REJECTED: ").toUpperCase();

        String error;

        switch (decision) {
            case "APPROVED" -> error = borrowingDao.approveRequest(id, manager.userId());
            case "REJECTED" -> error = borrowingDao.rejectRequest(id, manager.userId());
            default -> {
                System.out.println("Invalid decision. Enter APPROVED or REJECTED.");
                return;
            }
        }

        System.out.println(error == null
                ? "Request " + decision.toLowerCase() + "."
                : "Not changed: " + error);
    }

    private void printTransactions(List<BorrowingTransaction> list) {
        System.out.printf("%-6s | %-6s | %-8s | %-6s | %-12s%n", "ReqID", "UserID", "EquipID", "Qty", "Due Date");
        line(50);

        for (BorrowingTransaction t : list) {
            System.out.printf("%-6d | %-6d | %-8d | %-6d | %-12s%n",
                    t.borrowingId(), t.userId(), t.equipmentId(), t.quantity(), t.dueDate());
        }
    }

    // ---------- RETURNS & AUTOMATIC PENALTIES ----------

    private void recordReturn() {
        int id = readIntInput("Borrowing ID being returned (0 to cancel): ");

        if (id == 0) {
            return;
        }

        BorrowingTransaction tx = borrowingDao.findById(id);

        if (tx == null) {
            System.out.println("No borrowing record with ID " + id + ".");
            return;
        }

        if (!"APPROVED".equals(tx.requestStatus()) || !"NOT_RETURNED".equals(tx.returnStatus())) {
            System.out.println("This borrowing cannot be returned (status: " + tx.requestStatus()
                    + ", return: " + tx.returnStatus() + ").");
            return;
        }

        String today = LocalDate.now().toString();
        String returnDate = readLine(sc, "Return date (YYYY-MM-DD) [Enter for today " + today + "]: ");

        if (returnDate.isBlank()) {
            returnDate = today;
        }

        try {
            LocalDate.parse(returnDate);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date. Use the format YYYY-MM-DD.");
            return;
        }

        int missing;
        do {
            missing = readIntInput("Units MISSING (0 if none): ");
            if (missing > tx.quantity()) {
                System.out.println("Cannot exceed the borrowed quantity (" + tx.quantity() + ").");
            }
        } while (missing > tx.quantity());

        int damaged;
        do {
            damaged = readIntInput("Units returned DAMAGED (0 if none): ");
            if (damaged > tx.quantity() - missing) {
                System.out.println("Damaged units cannot exceed the units that came back ("
                        + (tx.quantity() - missing) + ").");
            }
        } while (damaged > tx.quantity() - missing);

        String error = borrowingDao.recordReturn(id, returnDate, missing, manager.userId());

        if (error != null) {
            System.out.println("Return not recorded: " + error);
            return;
        }

        System.out.println("Return recorded. Request marked COMPLETED.");

        // ---- Automatic penalty calculation ----
        Equipment eq = equipmentDao.findById(tx.equipmentId());
        double cost = eq == null ? 0 : eq.replacementCost();
        String name = eq == null ? "equipment" : eq.name();

        int lateDays = daysLate(tx.dueDate(), returnDate);

        if (lateDays > 0) {
            double amount = lateDays * LATE_FEE_PER_DAY;
            addAutoPenalty(id, "LATE",
                    lateDays + " day(s) late (due " + tx.dueDate() + ", returned " + returnDate + ")", amount);
        }

        if (damaged > 0) {
            addAutoPenalty(id, "DAMAGED",
                    damaged + " unit(s) of " + name + " returned damaged",
                    damaged * cost);
        }

        if (missing > 0) {
            addAutoPenalty(id, "MISSING",
                    missing + " unit(s) of " + name + " not returned",
                    missing * cost);
        }

        if (lateDays == 0 && damaged == 0 && missing == 0) {
            System.out.println("No penalties: returned on time and in full condition.");
        }
    }

    private void addAutoPenalty(int borrowingId, String type, String description, double amount) {
        if (penaltyDao.createPenalty(borrowingId, type, description, amount, manager.userId())) {
            System.out.printf("Penalty recorded: %s - %s - P%.2f%n", type, description, amount);
        } else {
            System.out.println("Failed to record " + type + " penalty.");
        }
    }

    private static int daysLate(String dueDate, String returnDate) {
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.parse(dueDate), LocalDate.parse(returnDate));
            return (int) Math.max(0, days);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    // ---------- PENALTIES ----------

    private void managePenalties() {
        List<Penalty> penalties = penaltyDao.getAllPenalties();

        System.out.println("\n================================= PENALTIES =================================");

        if (penalties.isEmpty()) {
            System.out.println("No penalties recorded.");
            return;
        }

        System.out.printf("%-6s | %-6s | %-8s | %-10s | %-8s | %-30s%n",
                "ID", "BorrID", "Type", "Amount", "Status", "Description");
        line(85);

        for (Penalty p : penalties) {
            System.out.printf("%-6d | %-6d | %-8s | P%-9.2f | %-8s | %-30s%n",
                    p.penaltyId(), p.borrowingId(), shorten(p.type(), 8), p.amount(),
                    shorten(p.status(), 8), shorten(p.description(), 30));
        }

        int id = readIntInput("Penalty ID to update (0 to cancel): ");

        if (id == 0) {
            return;
        }

        String status = readLine(sc, "Enter new status (PAID or UNPAID): ").toUpperCase();

        if (!status.equals("PAID") && !status.equals("UNPAID")) {
            System.out.println("Invalid status. Enter PAID or UNPAID.");
            return;
        }

        System.out.println(penaltyDao.updatePenaltyStatus(id, status, manager.userId())
                ? "Penalty status updated."
                : "Update failed. Check the Penalty ID.");
    }

    // ---------- REPORT ----------

    private void viewReport() {
        System.out.println("\n========== EQUIPMENT INVENTORY AND BORROWING REPORT ==========");

        for (Map.Entry<String, String> row : reportDao.metrics().entrySet()) {
            System.out.printf("%-20s : %s%n", row.getKey(), row.getValue());
        }

        List<BorrowingTransaction> overdue = borrowingDao.getOverdueTransactions();

        System.out.println("\n---------- OVERDUE EQUIPMENT ----------");

        if (overdue.isEmpty()) {
            System.out.println("No overdue equipment.");
        } else {
            printTransactions(overdue);
        }

        if (readLine(sc, "Export report to CSV? (y/n): ").equalsIgnoreCase("y")) {
            String path = "Equipment_Borrowing_Report.csv";
            if (reportDao.exportCsv(path)) {
                System.out.println("Report saved to " + Paths.get(path).toAbsolutePath());
            } else {
                System.out.println("Export failed.");
            }
        }
    }
}
