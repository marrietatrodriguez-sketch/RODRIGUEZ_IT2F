package com.scc.menu;

import java.util.Scanner;

/** Shared console helpers so every menu reads input and validates it the same way. */
public final class MenuUtil {

    private MenuUtil() {}

    public static String readLine(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /** Re-prompts until the user enters a non-negative integer. */
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
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a number.");
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
}
