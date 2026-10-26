
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

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
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // VIEW USERS
    private void displayUsers(List<User> users) {
        System.out.println("\n================ USER ACCOUNTS ================");

        if (users.isEmpty()) {
            System.out.println("No user accounts found.");
            return;
        }

        System.out.printf(
                "%-5s %-18s %-28s %-10s %-12s%n",
                "ID", "USERNAME", "EMAIL", "ROLE", "STATUS"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (User user : users) {
            System.out.printf(
                    "%-5d %-18s %-28s %-10s %-12s%n",
                    user.id(),
                    shorten(user.username(), 17),
                    shorten(user.email(), 27),
                    user.roles(),
                    user.status()
            );
        }
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() > maxLength
                ? value.substring(0, maxLength)
                : value;
    }

    // APPROVE REGISTRATION
    private void approveUser() {
        List<User> pendingUsers = userDao.getPendingUsers();

        displayUsers(pendingUsers);

        if (pendingUsers.isEmpty()) {
            return;
        }

        int id = readUserId("Enter the ID of the user to approve: ");

        if (id == 0) {
            return;
        }

        if (userDao.approveUser(id)) {
            System.out.println("User account approved successfully.");
        } else {
            System.out.println(
                    "Approval failed. Check the ID and account status."
            );
        }
    }

    // ADD USER
    private void addUser() {
        System.out.println("\n========== ADD USER ACCOUNT ==========");

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter email: ");
        String email = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println("All fields are required.");
            return;
        }

        String role = readRole();

        if (role == null) {
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

    // UPDATE USER
    private void updateUser() {
        displayUsers(userDao.getAllUsers());

        int id = readUserId("Enter the ID of the account to update: ");

        if (id == 0) {
            return;
        }

        User existingUser = findUser(id);

        if (existingUser == null) {
            System.out.println("User account not found.");
            return;
        }

        if ("DEACTIVATED".equalsIgnoreCase(existingUser.status())) {
            System.out.println("Cannot update a deactivated account.");
            return;
        }

        System.out.println("\nLeave username or email blank to keep the current value.");

        System.out.print(
                "New username [" + existingUser.username() + "]: "
        );
        String username = sc.nextLine().trim();

        if (username.isBlank()) {
            username = existingUser.username();
        }

        System.out.print(
                "New email [" + existingUser.email() + "]: "
        );
        String email = sc.nextLine().trim();

        if (email.isBlank()) {
            email = existingUser.email();
        }

        String role = existingUser.roles();

        System.out.print("Change role? (Y/N): ");
        String answer = sc.nextLine().trim();

        if (answer.equalsIgnoreCase("Y")) {
            String selectedRole = readRole();

            if (selectedRole == null) {
                return;
            }

            role = selectedRole;
        }

        if (userDao.updateUser(id, username, email, role)) {
            System.out.println("User account updated successfully.");
        } else {
            System.out.println(
                    "Update failed. Username or email may already exist."
            );
        }
    }

    // DEACTIVATE USER
    private void deactivateUser() {
        displayUsers(userDao.getAllUsers());

        int id = readUserId("Enter the ID of the account to deactivate: ");

        if (id == 0) {
            return;
        }

        User user = findUser(id);

        if (user == null) {
            System.out.println("User account not found.");
            return;
        }

        if ("DEACTIVATED".equalsIgnoreCase(user.status())) {
            System.out.println("This account is already deactivated.");
            return;
        }

        System.out.print(
                "Are you sure you want to deactivate "
                        + user.username() + "? (Y/N): "
        );

        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Deactivation cancelled.");
            return;
        }

        if (userDao.deactivateUser(id)) {
            System.out.println("User account deactivated successfully.");
        } else {
            System.out.println("Failed to deactivate the account.");
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

    // READ USER ID
    private int readUserId(String message) {
        System.out.print(message);

        try {
            int id = Integer.parseInt(sc.nextLine().trim());

            if (id <= 0) {
                System.out.println("Please enter a valid positive ID.");
                return 0;
            }

            return id;

        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric ID.");
            return 0;
        }
    }

    // SELECT ROLE
    private String readRole() {
        System.out.println("\nSelect User Role:");
        System.out.println("1. ADMIN");
        System.out.println("2. MANAGER");
        System.out.println("3. USER");
        System.out.print("Enter your choice: ");

        return switch (sc.nextLine().trim()) {
            case "1" -> "ADMIN";
            case "2" -> "MANAGER";
            case "3" -> "USER";
            default -> {
                System.out.println("Invalid role selected.");
                yield null;
            }
        };
    }
}
