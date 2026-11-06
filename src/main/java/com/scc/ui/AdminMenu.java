
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, Scanner sc, User currentAdmin) {
        this.userDao = userDao;
        this.sc = sc;
        this.currentAdmin = currentAdmin;
    }

    // ADMIN MENU
    public void start() {

        while (true) {

            System.out.println("\n======================================");
            System.out.println("              ADMIN MENU");
            System.out.println("======================================");
            System.out.println("1. View All User Accounts");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Add User Account");
            System.out.println("5. Update User Account");
            System.out.println("6. Deactivate User Account");
            System.out.println("7. Log Out");
            System.out.println("======================================");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> displayUsers(userDao.getAllUsers());
                case "2" -> displayUsers(userDao.getPendingUsers());
                case "3" -> approveUser();
                case "4" -> addUser();
                case "5" -> updateUser();
                case "6" -> deactivateUser();

                case "7" -> {
                    System.out.println("Logging out...");
                    return;
                }

                default -> System.out.println(
                        "Invalid choice. Please try again."
                );
            }
        }
    }

    // DISPLAY ALL USERS
    private void displayUsers(List<User> users) {

        System.out.println("\n================ USER ACCOUNTS ================");

        if (users.isEmpty()) {
            System.out.println("No user accounts found.");
            return;
        }

        System.out.println(
                "+------+----------------+------------+------------+"
        );

        System.out.printf(
                "| %-4s | %-14s | %-10s | %-10s |%n",
                "ID", "Username", "Role", "Status"
        );

        System.out.println(
                "+------+----------------+------------+------------+"
        );

        for (User user : users) {

            System.out.printf(
                    "| %-4d | %-14s | %-10s | %-10s |%n",
                    user.id(),
                    shorten(user.username(), 14),
                    user.roles(),
                    user.status()
            );
        }

        System.out.println(
                "+------+----------------+------------+------------+"
        );
    }

    // SHORTEN LONG TEXT
    private String shorten(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        return value.length() > maxLength
                ? value.substring(0, maxLength)
                : value;
    }

    // APPROVE USER ACCOUNT
    private void approveUser() {

        List<User> pendingUsers = userDao.getPendingUsers();

        displayUsers(pendingUsers);

        if (pendingUsers.isEmpty()) {
            return;
        }

        int id = readIntInput(
                "Enter User ID to approve (0 to cancel): "
        );

        if (id == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        if (userDao.approveUser(id)) {

            System.out.println(
                    "User account approved successfully."
            );

        } else {

            System.out.println(
                    "Approval failed. Check the ID and account status."
            );
        }
    }

    // ADD USER ACCOUNT
    private void addUser() {

        System.out.println("\n========== ADD USER ACCOUNT ==========");

        System.out.print("Enter username (0 to cancel): ");
        String username = sc.nextLine().trim();

        if (username.equals("0")) {
            System.out.println("Adding user cancelled.");
            return;
        }

        System.out.print("Enter email (0 to cancel): ");
        String email = sc.nextLine().trim();

        if (email.equals("0")) {
            System.out.println("Adding user cancelled.");
            return;
        }

        System.out.print("Enter password (0 to cancel): ");
        String password = sc.nextLine();

        if (password.equals("0")) {
            System.out.println("Adding user cancelled.");
            return;
        }

        if (username.isBlank() || email.isBlank() || password.isBlank()) {

            System.out.println("All fields are required.");
            return;
        }

        String role = readValidRole();

        if (role == null) {
            System.out.println("Adding user cancelled.");
            return;
        }

        if (userDao.addUser(username, email, password, role)) {

            System.out.println(
                    "User account added successfully and approved."
            );

        } else {

            System.out.println(
                    "Failed to add account. Username or email may already exist."
            );
        }
    }

    // UPDATE USER ROLE AND STATUS
    private void updateUser() {

        List<User> users = userDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No user accounts found.");
            return;
        }

        System.out.println("\n--- UPDATE USER ROLE AND STATUS ---");

        displayUsers(users);

        User targetUser;

        // SELECT TARGET USER
        while (true) {

            int targetUserId = readIntInput(
                    "Enter User ID to update (0 to cancel): "
            );

            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            // PREVENT ADMIN FROM MODIFYING OWN ACTIVE ACCOUNT
            if (currentAdmin != null
                    && targetUserId == currentAdmin.id()
                    && "APPROVED".equalsIgnoreCase(currentAdmin.status())) {

                System.out.println(
                        "WARNING: Modifying the currently active admin account is restricted."
                );

                continue;
            }

            targetUser = findUser(targetUserId);

            if (targetUser == null) {

                System.out.println(
                        "User ID not found. Please try again."
                );

                continue;
            }

            if ("DEACTIVATED".equalsIgnoreCase(targetUser.status())) {

                System.out.println(
                        "This account is deactivated and cannot be updated."
                );

                continue;
            }

            break;
        }

        // DISPLAY SELECTED USER
        System.out.println("\nSelected User: " + targetUser.username());
        System.out.println("Current Role: " + targetUser.roles());
        System.out.println("Current Status: " + targetUser.status());

        // GET NEW ROLE
        String newRole = readValidRole();

        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // GET NEW STATUS
        String newStatus = readValidStatus();

        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // CONFIRM UPDATE
        System.out.println("\n========== UPDATE CONFIRMATION ==========");
        System.out.println("User ID: " + targetUser.id());
        System.out.println("Username: " + targetUser.username());
        System.out.println("New Role: " + newRole);
        System.out.println("New Status: " + newStatus);
        System.out.println("==========================================");

        System.out.print("Enter Y to confirm or 0 to cancel: ");

        String confirmation = sc.nextLine().trim();

        if (confirmation.equals("0")
                || !confirmation.equalsIgnoreCase("Y")) {

            System.out.println("Update cancelled.");
            return;
        }

        // SAVE CHANGES TO DATABASE
        if (userDao.updateUserRoleAndStatus(
                targetUser.id(),
                newRole,
                newStatus)) {

            System.out.println(
                    "\nUser '" + targetUser.username()
                            + "' (ID " + targetUser.id()
                            + ") updated successfully: role = "
                            + newRole
                            + ", status = "
                            + newStatus + "."
            );

        } else {

            System.out.println(
                    "Failed to update user account."
            );
        }
    }

    // DEACTIVATE USER ACCOUNT
    private void deactivateUser() {

        displayUsers(userDao.getAllUsers());

        int id = readIntInput(
                "Enter User ID to deactivate (0 to cancel): "
        );

        if (id == 0) {
            System.out.println("Deactivation cancelled.");
            return;
        }

        User user = findUser(id);

        if (user == null) {
            System.out.println("User account not found.");
            return;
        }

        if (id == currentAdmin.id()) {
            System.out.println("You cannot deactivate your own account.");
            return;
        }

        if ("DEACTIVATED".equalsIgnoreCase(user.status())) {

            System.out.println(
                    "This account is already deactivated."
            );

            return;
        }

        System.out.print(
                "Are you sure you want to deactivate "
                        + user.username()
                        + "? (Y/N): "
        );

        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {

            System.out.println("Deactivation cancelled.");
            return;
        }

        if (userDao.deactivateUser(id)) {

            System.out.println(
                    "User account deactivated successfully."
            );

        } else {

            System.out.println(
                    "Failed to deactivate the account."
            );
        }
    }

    // FIND USER BY ID
    private User findUser(int id) {

        for (User user : userDao.getAllUsers()) {

            if (user.id() == id) {
                return user;
            }
        }

        return null;
    }

    // INTEGER INPUT VALIDATION
    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = sc.nextLine().trim();

            try {

                int id = Integer.parseInt(input);

                if (id >= 0) {
                    return id;
                }

                System.out.println(
                        "Invalid input. Please enter a positive number or 0."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a number."
                );
            }
        }
    }

    // ROLE VALIDATION
    private String readValidRole() {

        while (true) {

            System.out.println(
                    "Valid roles: ADMIN, MANAGER, USER"
            );

            System.out.print("Enter new role (0 to cancel): ");

            String role = sc.nextLine().trim().toUpperCase();

            if (role.equals("0")) {
                return null;
            }

            if (role.equals("ADMIN")
                    || role.equals("MANAGER")
                    || role.equals("USER")) {

                return role;
            }

            System.out.println(
                    "Invalid role. Please enter ADMIN, MANAGER, or USER."
            );
        }
    }

    // STATUS VALIDATION
    private String readValidStatus() {

        while (true) {

            System.out.println(
                    "Valid statuses: APPROVED, PENDING"
            );

            System.out.print("Enter new status (0 to cancel): ");

            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0")) {
                return null;
            }

            if (status.equals("APPROVED")
                    || status.equals("PENDING")) {

                return status;
            }

            System.out.println(
                    "Invalid status. Please enter APPROVED or PENDING."
            );
        }
    }
}
