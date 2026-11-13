
package com.scc.menu;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.BorrowingDao;
import com.scc.dao.EquipmentDao;
import com.scc.dao.PenaltyDao;
import com.scc.dao.ReportDao;

import com.scc.model.ActivityLog;
import com.scc.model.BorrowingTransaction;
import com.scc.model.Equipment;
import com.scc.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static com.scc.menu.MenuUtil.*;

public class ManagerMenu {

    private final EquipmentDao equipmentDao;
    private final BorrowingDao borrowingDao;
    private final PenaltyDao penaltyDao;
    private final ReportDao reportDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner sc;
    private final User manager;

    public ManagerMenu(Scanner sc, User manager) {
        this.sc = sc;
        this.manager = manager;

        this.equipmentDao = new EquipmentDao();
        this.borrowingDao = new BorrowingDao();
        this.penaltyDao = new PenaltyDao();
        this.reportDao = new ReportDao();
        this.activityLogDao = new ActivityLogDao();
    }

    public void start() {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("           MANAGER DASHBOARD");
            System.out.println("========================================");
            System.out.println("Welcome, " + manager.username());
            System.out.println("1. Manage Equipment Inventory");
            System.out.println("2. Review Borrowing Requests (approve / reject)");
            System.out.println("3. Record Equipment Return & Penalties");
            System.out.println("4. View Equipment & Borrowing Report");
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Logout");
            System.out.println("========================================");

            switch (readLine(sc, "Enter your choice: ")) {
                case "1" -> manageEquipment();
                case "2" -> reviewRequests();
                case "3" -> recordReturn();
                case "4" -> System.out.println(
                        "\n" + reportDao.buildSummary()
                );
                case "5" -> displayLogs(activityLogDao.getAllLogs());
                case "6" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void manageEquipment() {
        while (true) {
            System.out.println("\n================ EQUIPMENT INVENTORY ================");
            System.out.println("1. View Equipment List");
            System.out.println("2. Search Equipment");
            System.out.println("3. Add Equipment");
            System.out.println("4. Update Equipment");
            System.out.println("5. Delete Equipment");
            System.out.println("6. Back");
            System.out.println("=====================================================");

            switch (readLine(sc, "Enter choice: ")) {
                case "1" -> displayEquipment(
                        equipmentDao.getAllEquipment()
                );

                case "2" -> displayEquipment(
                        equipmentDao.searchEquipment(
                                readLine(sc, "Search (name/category/condition): ")
                        )
                );

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

        System.out.printf(
                "%-5s | %-18s | %-12s | %-6s | %-6s | %-10s | %-10s%n",
                "ID", "Name", "Category", "Qty", "Avail", "Condition", "Cost"
        );

        line(85);

        for (Equipment eq : list) {
            System.out.printf(
                    "%-5d | %-18s | %-12s | %-6d | %-6d | %-10s | P%-9.2f%n",
                    eq.equipmentId(),
                    shorten(eq.name(), 18),
                    shorten(eq.category(), 12),
                    eq.quantity(),
                    eq.availableQuantity(),
                    eq.condition(),
                    eq.replacementCost()
            );
        }
    }

    private void addEquipment() {
        String name = readLine(sc, "Equipment name: ");
        String category = readLine(sc, "Category: ");
        int qty = readInt(sc, "Quantity: ");
        String condition = readLine(sc, "Condition (e.g. Good, Fair): ");
        double cost = readDouble(sc, "Replacement cost (P): ");

        System.out.println(
                equipmentDao.addEquipment(
                        name,
                        category,
                        qty,
                        condition,
                        cost,
                        manager.userId()
                )
                        ? "Equipment added."
                        : "Failed to add equipment."
        );
    }

    private void updateEquipment() {
        displayEquipment(equipmentDao.getAllEquipment());

        int id = readInt(sc, "Equipment ID to update (0 to cancel): ");

        if (id == 0) {
            return;
        }

        int qty = readInt(sc, "New total quantity: ");
        int avail = readInt(sc, "New available quantity: ");
        String condition = readLine(sc, "New condition: ");
        double cost = readDouble(sc, "New replacement cost (P): ");

        System.out.println(
                equipmentDao.updateEquipment(
                        id,
                        qty,
                        avail,
                        condition,
                        cost,
                        manager.userId()
                )
                        ? "Equipment updated."
                        : "Update failed."
        );
    }

    private void deleteEquipment() {
        displayEquipment(equipmentDao.getAllEquipment());

        int id = readInt(sc, "Equipment ID to delete (0 to cancel): ");

        if (id == 0) {
            return;
        }

        System.out.println(
                equipmentDao.deleteEquipment(id, manager.userId())
                        ? "Equipment deleted."
                        : "Delete failed (check the ID or existing borrowings)."
        );
    }

    private void reviewRequests() {
        List<BorrowingTransaction> pending =
                borrowingDao.getPendingRequests();

        if (pending.isEmpty()) {
            System.out.println("No pending borrowing requests.");
            return;
        }

        System.out.printf(
                "%-6s | %-6s | %-8s | %-6s | %-12s%n",
                "ReqID", "UserID", "EquipID", "Qty", "Due Date"
        );

        line(50);

        for (BorrowingTransaction t : pending) {
            System.out.printf(
                    "%-6d | %-6d | %-8d | %-6d | %-12s%n",
                    t.borrowingId(),
                    t.userId(),
                    t.equipmentId(),
                    t.quantity(),
                    t.dueDate()
            );
        }

        int id = readInt(sc, "Request ID to act on (0 to cancel): ");

        if (id == 0) {
            return;
        }

        String decision = readLine(
                sc,
                "Enter APPROVED or REJECTED: "
        ).toUpperCase();

        if (!decision.equals("APPROVED")
                && !decision.equals("REJECTED")) {
            System.out.println("Invalid decision.");
            return;
        }

        System.out.println(
                borrowingDao.updateRequestStatus(
                        id,
                        decision,
                        manager.userId()
                )
                        ? "Request " + decision.toLowerCase() + "."
                        : "Update failed."
        );
    }

    private void recordReturn() {
        int borrowingId = readInt(
                sc,
                "Borrowing ID being returned (0 to cancel): "
        );

        if (borrowingId == 0) {
            return;
        }

        String returnDate = readLine(
                sc,
                "Return date (YYYY-MM-DD) [" + LocalDate.now() + "]: "
        );

        if (returnDate.isBlank()) {
            returnDate = LocalDate.now().toString();
        }

        if (!borrowingDao.recordReturn(
                borrowingId,
                returnDate,
                manager.userId()
        )) {
            System.out.println(
                    "Failed to record return. Check the Borrowing ID."
            );
            return;
        }

        System.out.println("Return recorded.");

        String damaged = readLine(
                sc,
                "Was any item damaged or missing? (y/n): "
        ).toLowerCase();

        if (damaged.equals("y")) {
            String type = readLine(
                    sc,
                    "Penalty type (DAMAGED or MISSING): "
            ).toUpperCase();

            String description = readLine(sc, "Description: ");
            double amount = readDouble(sc, "Penalty amount (P): ");

            System.out.println(
                    penaltyDao.createPenalty(
                            borrowingId,
                            type,
                            description,
                            amount,
                            manager.userId()
                    )
                            ? "Penalty recorded."
                            : "Failed to record penalty."
            );
        }
    }

    private void displayLogs(List<ActivityLog> logs) {
        System.out.println(
                "\n============================== SYSTEM AUDIT LOGS =============================="
        );

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID", "User ID", "Action Type", "Description", "Timestamp"
        );

        line(92);

        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        for (ActivityLog log : logs) {
            System.out.printf(
                    "%-5d | %-8s | %-18s | %-35s | %-20s%n",
                    log.logId(),
                    String.valueOf(log.userId()),
                    shorten(log.action(), 18),
                    shorten(log.description(), 35),
                    shorten(log.dateTime(), 20)
            );
        }

        line(92);
    }
}
