
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void start() {
        while (true) {
            System.out.println("\n======================================");
            System.out.println("  EVENT EQUIPMENT BORROWING SYSTEM");
            System.out.println("======================================");
            System.out.println("1. Log In");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.println("======================================");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    System.out.println("Thank you for using the system!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // LOGIN
    private void login() {
        System.out.println("\n========== LOG IN ==========");

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        if (username.isBlank() || password.isBlank()) {
            System.out.println("Username and password are required.");
            return;
        }

        User user = userDao.getUserByUsername(username);

        if (user == null || !user.password().equals(password)) {
            System.out.println("Incorrect username or password.");
            return;
        }

        if ("DEACTIVATED".equalsIgnoreCase(user.status())) {
            System.out.println("Your account has been deactivated.");
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println("Your account is still pending approval.");
            System.out.println("Please wait for the administrator to approve it.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username() + "!");

        routeUser(user);
    }

    // REGISTRATION
    private void register() {
        System.out.println("\n========== REGISTER ==========");

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

        // Public registration creates USER accounts only.
        // The account must be approved before login.
        boolean registered = userDao.registerUser(
                username,
                email,
                password,
                "USER"
        );

        if (registered) {
            System.out.println("\nRegistration successful!");
            System.out.println("Your account status is PENDING.");
            System.out.println("Please wait for administrator approval.");
        } else {
            System.out.println("Registration failed.");
            System.out.println("The username or email may already exist.");
        }
    }

    // ROLE ROUTING
    private void routeUser(User user) {
        if ("ADMIN".equalsIgnoreCase(user.roles())) {
            new AdminMenu(userDao, sc).start();
        } else {
            System.out.println("\nYour role is: " + user.roles());
            System.out.println("Your dashboard has not been implemented yet.");
        }
    }
}
