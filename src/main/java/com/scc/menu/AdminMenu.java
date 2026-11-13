
package com.scc.menu;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

import static com.scc.menu.MenuUtil.*;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner sc;
    private final User admin;

    public AdminMenu(UserDao userDao, ActivityLogDao activityLogDao,
                     Scanner sc, User admin) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.sc = sc;
        this.admin = admin;
    }

    public void start() {
        while (true) {
            System.out.println("\n======================================");
            System.out.println("             ADMIN MENU");
            System.out.println("======================================");
            System.out.println("Welcome, " + admin.username());
            System.out.println("1. View All User Accounts");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Add User Account");
            System.out.println("5. Update User Role/Status");
            System.out.println("6. Deactivate User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Logout");
            System.out.println("======================================");

            switch (readLine(sc, "Enter your choice: ")) {
                case "1" -> displayUsers(userDao.getAllUsers());
                case "2" -> displayUsers(userDao.getPendingUsers());
                case "3" -> approveUser();
                case "4" -> addUser();
                case "5" -> updateUser();
                case "6" -> deactivateUser();
                case "7" -> auditLogSubmenu();
                case "8" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void auditLogSubmenu() {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("         SYSTEM AUDIT LOG MENU");
            System.out.println("========================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs (requires confirmation)");
            System.out.println("4. Back to Admin Menu");
            System.out.println("========================================");

            switch (readLine(sc, "Enter your choice: ")) {
                case "1" -> displayLogs(activityLogDao.getAllLogs());

                case "2" -> {
                    int id = readInt(sc, "Enter User ID to filter (0 to cancel): ");
                    if (id > 0) {
                        displayLogs(activityLogDao.getLogsByUserId(id));
                    }
                }

                case "3" -> clearLogs();

                case "4" -> {
                    return;
                }

                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void clearLogs() {
        String confirm = readLine(
                sc,
                "Type CLEAR to permanently delete all audit logs: "
        );

        if (!"CLEAR".equals(confirm)) {
            System.out.println("Cancelled. No logs were deleted.");
            return;
        }

        if (activityLogDao.clearAllLogs()) {
            activityLogDao.logAction(
                    admin.userId(),
                    "LOGS_CLEARED",
                    "Admin " + admin.userId() + " cleared all audit logs."
            );

            System.out.println("Audit logs cleared.");
        } else {
            System.out.println("Failed to clear logs.");
        }
    }

    private void displayUsers(List<User> users) {
        System.out.println("\n================ USER ACCOUNTS ================");

        if (users.isEmpty()) {
            System.out.println("No user accounts found.");
            return;
        }

        System.out.printf(
                "%-6s | %-18s | %-12s | %-12s%n",
                "ID", "Username", "Role", "Status"
        );

        line(56);

        for (User u : users) {
            System.out.printf(
                    "%-6d | %-18s | %-12s | %-12s%n",
                    u.userId(),
                    shorten(u.username(), 18),
                    u.role(),
                    u.status()
            );
        }
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();

        displayUsers(pending);

        if (pending.isEmpty()) {
            return;
        }

        int id = readInt(sc, "Enter User ID to approve (0 to cancel): ");

        if (id == 0) {
            return;
        }

        System.out.println(
                userDao.approveUser(id, admin.userId())
                        ? "User account approved."
                        : "Approval failed. Check the ID."
        );
    }

    private void addUser() {
        System.out.println("\n========== ADD USER ACCOUNT ==========");

        String username = readLine(sc, "Enter username: ");
        String email = readLine(sc, "Enter email: ");
        String password = readLine(sc, "Enter password: ");

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println("All fields are required.");
            return;
        }

        String role = readLine(
                sc,
                "Enter role (ADMIN, MANAGER, USER): "
        ).toUpperCase();

        if (!List.of("ADMIN", "MANAGER", "USER").contains(role)) {
            System.out.println("Invalid role.");
            return;
        }

        System.out.println(
                userDao.addUser(
                        username,
                        email,
                        password,
                        role,
                        admin.userId()
                )
                        ? "User added."
                        : "Failed to add user (username or email may exist)."
        );
    }

    private void updateUser() {
        displayUsers(userDao.getAllUsers());

        int id = readInt(sc, "Enter User ID to update (0 to cancel): ");

        if (id == 0) {
            return;
        }

        String role = readLine(
                sc,
                "Enter role (ADMIN, MANAGER, USER): "
        ).toUpperCase();

        String status = readLine(
                sc,
                "Enter status (APPROVED, PENDING): "
        ).toUpperCase();

        if (!List.of("ADMIN", "MANAGER", "USER").contains(role)
                || !List.of("APPROVED", "PENDING").contains(status)) {

            System.out.println("Invalid role or status.");
            return;
        }

        System.out.println(
                userDao.updateUserRoleAndStatus(
                        id,
                        role,
                        status,
                        admin.userId()
                )
                        ? "User updated."
                        : "Update failed."
        );
    }

    private void deactivateUser() {
        displayUsers(userDao.getAllUsers());

        int id = readInt(sc, "Enter User ID to deactivate (0 to cancel): ");

        if (id == 0) {
            return;
        }

        System.out.println(
                userDao.deactivateUser(id, admin.userId())
                        ? "User deactivated."
                        : "Deactivation failed."
        );
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
