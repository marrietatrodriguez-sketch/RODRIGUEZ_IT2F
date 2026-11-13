package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DatabaseInitializer {

    // Matches the ER diagram / proposal: Log_ID, User_ID, Action, Description, Date_Time
    static final String ACTIVITY_LOGS_DDL =
            "CREATE TABLE IF NOT EXISTS Activity_Logs (" +
                    "Log_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "User_ID INTEGER, " +
                    "Action TEXT NOT NULL, " +
                    "Description TEXT NOT NULL, " +
                    "Date_Time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (User_ID) REFERENCES Users(User_ID) " +
                    "ON DELETE CASCADE)";

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

                // ACTIVITY LOGS TABLE (activity_logs)
                ACTIVITY_LOGS_DDL
        };

        try (
                Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()
        ) {

            stmt.execute("PRAGMA foreign_keys = ON");

            for (String ddl : tables) {
                stmt.execute(ddl);
            }

            migrateActivityLogs(conn);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Cannot start without database initialization.", e
            );
        }
    }

    // Older builds created Activity_Logs with action_type / dateTime columns.
    // Rebuild the table once so it matches the proposal and keeps all existing rows.
    private static void migrateActivityLogs(Connection conn) throws SQLException {

        Set<String> columns = new HashSet<>();

        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(Activity_Logs)")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }

        if (!columns.contains("action_type") && !columns.contains("datetime")) {
            return;
        }

        try (Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE Activity_Logs RENAME TO Activity_Logs_old");
            s.execute(ACTIVITY_LOGS_DDL);
            s.execute("INSERT INTO Activity_Logs (Log_ID, User_ID, Action, Description, Date_Time) "
                    + "SELECT Log_ID, User_ID, action_type, Description, dateTime FROM Activity_Logs_old");
            s.execute("DROP TABLE Activity_Logs_old");
        }
    }
}
