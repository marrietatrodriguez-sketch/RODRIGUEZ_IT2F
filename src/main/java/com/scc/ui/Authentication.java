
package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, ActivityLogDao activityLogDao, Scanner sc) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
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

            switch (MenuUtil.readLine(sc, "Enter your choice: ")) {

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

        String username = MenuUtil.readLine(sc, "Username: ");
        String password = MenuUtil.readLine(sc, "Password: ");

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
            return;
        }

        // Record login activity
        activityLogDao.logAction(
                user.userId(),
                "LOGIN",
                "User " + user.username() + " (ID: " + user.userId() + ") logged in."
        );

        System.out.println("\nLogin successful! Welcome, " + user.username() + "!");

        route(user);

        // Record logout activity
        activityLogDao.logAction(
                user.userId(),
                "LOGOUT",
                "User " + user.username() + " (ID: " + user.userId() + ") logged out."
        );
    }

    // REGISTER
    private void register() {

        System.out.println("\n========== REGISTER ==========");

        String username = MenuUtil.readLine(sc, "Username: ");
        String email = MenuUtil.readLine(sc, "Email: ");
        String password = MenuUtil.readLine(sc, "Password: ");

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println("All fields are required.");
            return;
        }

        System.out.println("\n========== SELECT ACCOUNT ROLE ==========");
        System.out.println("1. USER");
        System.out.println("2. MANAGER");
        System.out.println("3. ADMIN");
        System.out.println("=========================================");

        String roleChoice = MenuUtil.readLine(sc, "Choose your role: ");

        String role;

        switch (roleChoice) {

            case "1" -> role = "USER";

            case "2" -> role = "MANAGER";

            case "3" -> role = "ADMIN";

            default -> {
                System.out.println("Invalid role selection. Registration cancelled.");
                return;
            }
        }

        boolean registered = userDao.registerUser(
                username,
                email,
                password,
                role
        );

        if (registered) {

            System.out.println("\nRegistration successful!");
            System.out.println("Selected Role: " + role);
            System.out.println("Account Status: PENDING");
            System.out.println("Please wait for administrator approval.");

        } else {

            System.out.println("\nRegistration failed.");
            System.out.println("Username or email may already exist.");
        }
    }

    // ROLE-BASED MENU ROUTING
    private void route(User user) {

        switch (user.role().toUpperCase()) {

            case "ADMIN" ->
                    new AdminMenu(userDao, activityLogDao, sc, user).start();

            case "MANAGER" ->
                    new ManagerMenu(sc, user).start();

            case "USER" ->
                    new UserMenu(userDao, sc, user).start();

            default ->
                    System.out.println("Invalid user role.");
        }
    }
}
