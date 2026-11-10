
package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // RECORD ACTIVITY LOG
    public boolean logAction(int userId, String actionType, String description) {

        String sql = "INSERT INTO Activity_Logs "
                + "(User_ID, action_type, Description) "
                + "VALUES (?, ?, ?)";

        try (
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            String finalDescription = description;

            // Retrieve user's role and username
            if (userId > 0) {

                String roleSql = "SELECT Role, Username FROM Users "
                        + "WHERE User_ID = ?";

                try (PreparedStatement roleStatement =
                             c.prepareStatement(roleSql)) {

                    roleStatement.setInt(1, userId);

                    try (ResultSet r = roleStatement.executeQuery()) {

                        if (r.next()) {

                            String role = r.getString("Role");
                            String username = r.getString("Username");

                            finalDescription = role.toUpperCase()
                                    + " " + username
                                    + " (ID: " + userId + ") "
                                    + description;
                        }
                    }
                }
            }

            // Set User_ID
            if (userId <= 0) {
                p.setNull(1, Types.INTEGER);
            } else {
                p.setInt(1, userId);
            }

            p.setString(2, actionType);
            p.setString(3, finalDescription);

            return p.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Log write failed: " + e.getMessage());
            return false;
        }
    }

    // GET ALL ACTIVITY LOGS
    public List<ActivityLog> getAllLogs() {

        String sql = "SELECT * FROM Activity_Logs "
                + "ORDER BY dateTime DESC, Log_ID DESC";

        return query(sql, null);
    }

    // GET ACTIVITY LOGS BY USER ID
    public List<ActivityLog> getLogsByUserId(int userId) {

        String sql = "SELECT * FROM Activity_Logs "
                + "WHERE User_ID = ? "
                + "ORDER BY dateTime DESC, Log_ID DESC";

        return query(sql, userId);
    }

    // QUERY ACTIVITY LOGS
    private List<ActivityLog> query(String sql, Integer uid) {

        List<ActivityLog> logs = new ArrayList<>();

        try (
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            if (uid != null) {
                p.setInt(1, uid);
            }

            try (ResultSet r = p.executeQuery()) {

                while (r.next()) {

                    int rawUserId = r.getInt("User_ID");
                    Integer userId = r.wasNull() ? null : rawUserId;

                    ActivityLog log = new ActivityLog(
                            r.getInt("Log_ID"),
                            userId,
                            r.getString("action_type"),
                            r.getString("Description"),
                            r.getString("dateTime")
                    );

                    logs.add(log);
                }
            }

        } catch (SQLException e) {
            System.err.println("Log read failed: " + e.getMessage());
        }

        return logs;
    }

    // CLEAR ALL ACTIVITY LOGS
    public boolean clearAllLogs() {

        String sql = "DELETE FROM Activity_Logs";

        try (
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Clearing logs failed: " + e.getMessage());
            return false;
        }
    }
}
