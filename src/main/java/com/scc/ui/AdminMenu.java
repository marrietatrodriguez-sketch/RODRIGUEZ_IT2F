package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

import static com.scc.ui.MenuUtil.*;

public class AdminMenu {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "MANAGER", "USER");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

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

    // ---------- INPUT VALIDATION WRAPPERS ----------

    /** Re-prompts on letters, symbols or blank input. Never throws. 0 is allowed (used for cancel). */
    private int readIntInput(String prompt) {
        while (true) {
            String input = readLine(sc, prompt);
            try {
                int value = Integer.parseInt(input);
                if (value >= 0) {
                    return value;
                }
                System.out.println("Please enter 0 or a positive whole number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input '" + input + "'. Numbers only, no letters or symbols.");
            }
        }
    }

    /** Keeps asking until a valid role is entered. Returns null if the admin enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            String role = readLine(sc, "Enter role (ADMIN, MANAGER, USER) or 0 to cancel: ").toUpperCase();
            if (role.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(role)) {
                return role;
            }
            System.out.println("Invalid role. Choose ADMIN, MANAGER or USER.");
        }
    }

    /** Keeps asking until a valid status is entered. Returns null if the admin enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            String status = readLine(sc, "Enter status (APPROVED, PENDING) or 0 to cancel: ").toUpperCase();
            if (status.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(status)) {
                return status;
            }
            System.out.println("Invalid status. Choose APPROVED or PENDING.");
        }
    }

    // ---------- AUDIT LOGS ----------

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
                case "1" -> printLogs(activityLogDao.getAllLogs());

                case "2" -> {
                    int id = readIntInput("Enter User ID to filter (0 to cancel): ");
                    if (id == 0) {
                        System.out.println("Filter cancelled.");
                    } else {
                        printLogs(activityLogDao.getLogsByUserId(id));
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
        String confirm = readLine(sc, "Type CLEAR to permanently delete all audit logs (anything else cancels): ");

        if (!"CLEAR".equals(confirm)) {
            System.out.println("Cancelled. No logs were deleted.");
            return;
        }

        if (activityLogDao.clearAllLogs()) {
            activityLogDao.logAction(admin.userId(), "LOGS_CLEARED",
                    "Admin " + admin.userId() + " cleared all audit logs.");
            System.out.println("Audit logs cleared.");
        } else {
            System.out.println("Failed to clear logs.");
        }
    }

    // ---------- USER ACCOUNTS ----------

    private void displayUsers(List<User> users) {
        System.out.println("\n================ USER ACCOUNTS ================");

        if (users.isEmpty()) {
            System.out.println("No user accounts found.");
            return;
        }

        System.out.printf("%-6s | %-18s | %-12s | %-12s%n", "ID", "Username", "Role", "Status");
        line(56);

        for (User u : users) {
            System.out.printf("%-6d | %-18s | %-12s | %-12s%n",
                    u.userId(), shorten(u.username(), 18), u.role(), u.status());
        }
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();

        displayUsers(pending);

        if (pending.isEmpty()) {
            return;
        }

        int id = readIntInput("Enter User ID to approve (0 to cancel): ");

        if (id == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        System.out.println(userDao.approveUser(id, admin.userId())
                ? "User account approved."
                : "Approval failed. Check the ID.");
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

        String role = readValidRole();

        if (role == null) {
            System.out.println("Add user cancelled.");
            return;
        }

        System.out.println(userDao.addUser(username, email, password, role, admin.userId())
                ? "User added."
                : "Failed to add user (username or email may exist).");
    }

    /**
     * Update flow (Deliverable 7). 0 cancels at any step.
     * An admin cannot modify their own active account.
     */
    private void updateUser() {
        List<User> users = userDao.getAllUsers();

        displayUsers(users);

        if (users.isEmpty()) {
            return;
        }

        int id = readIntInput("Enter target User ID to update (0 to cancel): ");

        if (id == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (id == admin.userId()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        User target = findUser(users, id);

        if (target == null) {
            System.out.println("No user account found with ID " + id + ".");
            return;
        }

        System.out.println("Editing " + target.username() + " (current role: " + target.role()
                + ", status: " + target.status() + ")");

        String role = readValidRole();

        if (role == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String status = readValidStatus();

        if (status == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(id, role, status)) {
            activityLogDao.logAction(admin.userId(), "USER_UPDATED",
                    "Admin " + admin.userId() + " changed role of User " + id
                            + " to " + role + " and status to " + status + ".");
            System.out.println("User updated successfully.");
        } else {
            System.out.println("Update failed.");
        }
    }

    private void deactivateUser() {
        List<User> users = userDao.getAllUsers();

        displayUsers(users);

        if (users.isEmpty()) {
            return;
        }

        int id = readIntInput("Enter User ID to deactivate (0 to cancel): ");

        if (id == 0) {
            System.out.println("Deactivation cancelled.");
            return;
        }

        if (id == admin.userId()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        System.out.println(userDao.deactivateUser(id, admin.userId())
                ? "User deactivated."
                : "Deactivation failed.");
    }

    private static User findUser(List<User> users, int id) {
        for (User u : users) {
            if (u.userId() == id) {
                return u;
            }
        }
        return null;
    }
}
