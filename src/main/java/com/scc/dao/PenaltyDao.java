package com.scc.dao;

import com.scc.model.Penalty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PenaltyDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean createPenalty(int borrowingId, String type, String description, double amount, int managerId) {
        String sql = "INSERT INTO Penalties (Borrowing_ID, Penalty_Type, Description, Amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowingId);
            ps.setString(2, type.toUpperCase());
            ps.setString(3, description);
            ps.setDouble(4, amount);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "PENALTY_CREATED",
                        type.toUpperCase() + " penalty P" + amount + " on Borrowing ID " + borrowingId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to create penalty: " + e.getMessage());
        }
        return false;
    }

    public boolean updatePenaltyStatus(int penaltyId, String status, int managerId) {
        String sql = "UPDATE Penalties SET Penalty_Status = ? WHERE Penalty_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, penaltyId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "PENALTY_UPDATED",
                        "Penalty ID " + penaltyId + " set to " + status.toUpperCase());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update penalty: " + e.getMessage());
        }
        return false;
    }

    public List<Penalty> getAllPenalties() {
        List<Penalty> list = new ArrayList<>();
        String sql = "SELECT Penalty_ID, Borrowing_ID, Penalty_Type, Description, Amount, Penalty_Status "
                + "FROM Penalties ORDER BY Penalty_ID DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Penalty(
                        rs.getInt("Penalty_ID"),
                        rs.getInt("Borrowing_ID"),
                        rs.getString("Penalty_Type"),
                        rs.getString("Description"),
                        rs.getDouble("Amount"),
                        rs.getString("Penalty_Status")));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch penalties: " + e.getMessage());
        }
        return list;
    }
}
