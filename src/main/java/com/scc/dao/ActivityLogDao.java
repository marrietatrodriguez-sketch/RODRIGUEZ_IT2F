package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final String SELECT_COLUMNS =
            "SELECT Log_ID, User_ID, Action, Description, Date_Time FROM Activity_Logs ";

    // RECORD ACTIVITY LOG (parameterized). Date_Time is stored in local time.
    public boolean logAction(int userId, String actionType, String description) {

        String sql = "INSERT INTO Activity_Logs (User_ID, Action, Description, Date_Time) "
                + "VALUES (?, ?, ?, datetime('now', 'localtime'))";

        try (
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {
            if (userId <= 0) {
                p.setNull(1, Types.INTEGER);
            } else {
                p.setInt(1, userId);
            }

            p.setString(2, actionType);
            p.setString(3, description);

            return p.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Log write failed: " + e.getMessage());
            return false;
        }
    }

    // ALL LOGS, newest first
    public List<ActivityLog> getAllLogs() {
        return query(SELECT_COLUMNS + "ORDER BY Date_Time DESC, Log_ID DESC", null);
    }

    // LOGS FOR ONE USER, newest first
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT_COLUMNS + "WHERE User_ID = ? ORDER BY Date_Time DESC, Log_ID DESC", userId);
    }

    private List<ActivityLog> query(String sql, Integer userId) {

        List<ActivityLog> logs = new ArrayList<>();

        try (
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement p = c.prepareStatement(sql)
        ) {
            if (userId != null) {
                p.setInt(1, userId);
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int rawUserId = r.getInt("User_ID");
                    Integer uid = r.wasNull() ? null : rawUserId;

                    logs.add(new ActivityLog(
                            r.getInt("Log_ID"),
                            uid,
                            r.getString("Action"),
                            r.getString("Description"),
                            r.getString("Date_Time")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Log read failed: " + e.getMessage());
        }

        return logs;
    }

    // CLEAR ALL LOGS
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
