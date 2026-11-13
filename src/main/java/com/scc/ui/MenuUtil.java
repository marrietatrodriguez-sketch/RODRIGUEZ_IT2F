package com.scc.ui;

import com.scc.model.ActivityLog;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Shared console helpers so every menu reads and validates input the same way. */
public final class MenuUtil {

    private MenuUtil() {}

    public static String readLine(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /** Re-prompts until the user enters a non-negative whole number. Never throws. */
    public static int readInt(Scanner sc, String prompt) {
        while (true) {
            String input = readLine(sc, prompt);
            try {
                int value = Integer.parseInt(input);
                if (value >= 0) return value;
                System.out.println("Please enter a positive number or 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid whole number.");
            }
        }
    }

    public static double readDouble(Scanner sc, String prompt) {
        while (true) {
            String input = readLine(sc, prompt);
            try {
                double value = Double.parseDouble(input);
                if (value >= 0) return value;
                System.out.println("Amount cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a number.");
            }
        }
    }

    /** Re-prompts until the user enters a real date in YYYY-MM-DD format. */
    public static String readDate(Scanner sc, String prompt) {
        while (true) {
            String input = readLine(sc, prompt);
            try {
                return LocalDate.parse(input).toString();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use the format YYYY-MM-DD (e.g. 2026-11-30).");
            }
        }
    }

    public static String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }

    public static void line(int width) {
        System.out.println("-".repeat(width));
    }

    /** Aligned audit-log table: %-5s | %-8s | %-18s | %-35s | %-20s */
    public static void printLogs(List<ActivityLog> logs) {

        System.out.println("\n============================ SYSTEM AUDIT LOGS ============================");

        System.out.printf("%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID", "User ID", "Action Type", "Description", "Timestamp");

        line(98);

        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        for (ActivityLog log : logs) {
            System.out.printf("%-5s | %-8s | %-18s | %-35s | %-20s%n",
                    String.valueOf(log.logId()),
                    log.userId() == null ? "-" : String.valueOf(log.userId()),
                    shorten(log.action(), 18),
                    shorten(log.description(), 35),
                    shorten(log.dateTime(), 20));
        }

        line(98);
    }
}
