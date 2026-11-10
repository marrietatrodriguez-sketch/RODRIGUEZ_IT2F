
package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {

        String[] tables = {

                // USERS TABLE
                "CREATE TABLE IF NOT EXISTS Users (" +
                        "User_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Username TEXT NOT NULL UNIQUE, " +
                        "Email TEXT NOT NULL UNIQUE, " +
                        "Password TEXT NOT NULL, " +
                        "Role TEXT NOT NULL, " +
                        "Status TEXT NOT NULL DEFAULT 'PENDING')",

                // EQUIPMENT TABLE
                "CREATE TABLE IF NOT EXISTS Equipment (" +
                        "Equipment_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Equipment_Name TEXT NOT NULL, " +
                        "Category TEXT NOT NULL, " +
                        "Quantity INTEGER NOT NULL DEFAULT 0, " +
                        "Available_Quantity INTEGER NOT NULL DEFAULT 0, " +
                        "Condition TEXT NOT NULL, " +
                        "Replacement_Cost REAL NOT NULL)",

                // EVENTS TABLE
                "CREATE TABLE IF NOT EXISTS Events (" +
                        "Event_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "User_ID INTEGER NOT NULL, " +
                        "Event_Name TEXT NOT NULL, " +
                        "Event_Date TEXT NOT NULL, " +
                        "Location TEXT NOT NULL, " +
                        "Number_of_Guests INTEGER NOT NULL, " +
                        "FOREIGN KEY (User_ID) REFERENCES Users(User_ID) " +
                        "ON DELETE CASCADE)",

                // BORROWING TRANSACTIONS TABLE
                "CREATE TABLE IF NOT EXISTS Borrowing_Transactions (" +
                        "Borrowing_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Event_ID INTEGER NOT NULL, " +
                        "User_ID INTEGER NOT NULL, " +
                        "Equipment_ID INTEGER NOT NULL, " +
                        "Quantity INTEGER NOT NULL, " +
                        "Request_Date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "Due_Date TEXT NOT NULL, " +
                        "Return_Date TEXT, " +
                        "Request_Status TEXT NOT NULL DEFAULT 'PENDING', " +
                        "Return_Status TEXT NOT NULL DEFAULT 'NOT_RETURNED', " +
                        "FOREIGN KEY (Event_ID) REFERENCES Events(Event_ID) " +
                        "ON DELETE CASCADE, " +
                        "FOREIGN KEY (User_ID) REFERENCES Users(User_ID) " +
                        "ON DELETE CASCADE, " +
                        "FOREIGN KEY (Equipment_ID) REFERENCES Equipment(Equipment_ID) " +
                        "ON DELETE CASCADE)",

                // PENALTIES TABLE
                "CREATE TABLE IF NOT EXISTS Penalties (" +
                        "Penalty_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "Borrowing_ID INTEGER NOT NULL, " +
                        "Penalty_Type TEXT NOT NULL, " +
                        "Description TEXT NOT NULL, " +
                        "Amount REAL NOT NULL, " +
                        "Penalty_Status TEXT NOT NULL DEFAULT 'UNPAID', " +
                        "FOREIGN KEY (Borrowing_ID) REFERENCES Borrowing_Transactions(Borrowing_ID) " +
                        "ON DELETE CASCADE)",

                // ACTIVITY LOGS TABLE
                "CREATE TABLE IF NOT EXISTS Activity_Logs (" +
                        "Log_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "User_ID INTEGER, " +
                        "action_type TEXT NOT NULL, " +
                        "Description TEXT NOT NULL, " +
                        "dateTime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (User_ID) REFERENCES Users(User_ID) " +
                        "ON DELETE SET NULL)"
        };

        try (
                Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()
        ) {

            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");

            // Create all tables
            for (String ddl : tables) {
                stmt.execute(ddl);
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Cannot start without database initialization.", e
            );
        }
    }
}
