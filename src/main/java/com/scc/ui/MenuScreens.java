
package com.scc.ui;

import com.scc.Main;
import java.util.Scanner;

public class MenuScreens {

    public void showMenu() {

        Scanner sc = Main.sc;

        System.out.println("\n===============================");
        System.out.println(" EQUIPMENT AND PENALTY RECORD ");
        System.out.println("===============================");
        System.out.println("1. Log in");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        String choice = sc.nextLine();

        switch (choice) {
            case "1":
                System.out.println("Login selected");
                break;

            case "2":
                System.out.print("Enter new username: ");
                String user = sc.nextLine();

                System.out.print("Enter email: ");
                String email = sc.nextLine();

                System.out.print("Enter Password: ");
                String pass = sc.nextLine();

                System.out.print("Select role [1] ADMIN [2] MANAGER [3] USER: ");

                String roleChoice = sc.nextLine();
                String role;

                switch (roleChoice) {
                    case "1":
                        role = "ADMIN";
                        break;
                    case "2":
                        role = "MANAGER";
                        break;
                    case "3":
                        role = "USER";
                        break;
                    default:
                        role = null;
                }

                if (role != null) {
                    Main.userDao.registerUser(user, email, pass, role);
                } else {
                    System.out.println("Invalid role selection");
                }
                break;

            case "3":
                System.out.println("Exiting program...");
                break;

            default:
                System.out.println("Invalid option");
        }
    }
}
