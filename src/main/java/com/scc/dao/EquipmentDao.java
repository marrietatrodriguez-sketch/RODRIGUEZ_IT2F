package com.scc.dao;

import com.scc.model.Equipment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private static final String COLUMNS =
            "Equipment_ID, Equipment_Name, Category, Quantity, Available_Quantity, Condition, Replacement_Cost";

    public boolean addEquipment(String name, String category, int qty, String condition,
                                double cost, int managerId) {
        String sql = "INSERT INTO Equipment (Equipment_Name, Category, Quantity, Available_Quantity, Condition, Replacement_Cost) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, qty);
            ps.setInt(4, qty);
            ps.setString(5, condition);
            ps.setDouble(6, cost);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "EQUIPMENT_ADDED",
                        "Manager " + managerId + " added equipment '" + name + "' (Qty: " + qty + ").");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to add equipment: " + e.getMessage());
        }
        return false;
    }

    public boolean updateEquipment(int equipmentId, int qty, int available, String condition,
                                   double cost, int managerId) {
        String sql = "UPDATE Equipment SET Quantity = ?, Available_Quantity = ?, Condition = ?, Replacement_Cost = ? "
                + "WHERE Equipment_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, available);
            ps.setString(3, condition);
            ps.setDouble(4, cost);
            ps.setInt(5, equipmentId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "EQUIPMENT_UPDATED",
                        "Manager " + managerId + " updated Equipment ID " + equipmentId
                                + " (Qty: " + qty + ", Available: " + available + ", Condition: " + condition + ").");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update equipment: " + e.getMessage());
        }
        return false;
    }

    // Equipment that is still pending or out on loan cannot be deleted.
    public boolean deleteEquipment(int equipmentId, int managerId) {
        String sql = "DELETE FROM Equipment WHERE Equipment_ID = ? AND NOT EXISTS ("
                + "SELECT 1 FROM Borrowing_Transactions "
                + "WHERE Equipment_ID = ? AND Return_Status = 'NOT_RETURNED' "
                + "AND Request_Status IN ('PENDING', 'APPROVED'))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, equipmentId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "EQUIPMENT_DELETED",
                        "Manager " + managerId + " deleted Equipment ID " + equipmentId + ".");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete equipment: " + e.getMessage());
        }
        return false;
    }

    public List<Equipment> getAllEquipment() {
        return fetch("SELECT " + COLUMNS + " FROM Equipment ORDER BY Equipment_ID", null, null);
    }

    // Searches name, category, condition and quantity
    public List<Equipment> searchEquipment(String keyword) {
        return fetch("SELECT " + COLUMNS + " FROM Equipment WHERE Equipment_Name LIKE ? OR Category LIKE ? "
                + "OR Condition LIKE ? OR CAST(Quantity AS TEXT) LIKE ? ORDER BY Equipment_ID",
                "%" + keyword + "%", null);
    }

    public Equipment findById(int equipmentId) {
        List<Equipment> list = fetch("SELECT " + COLUMNS + " FROM Equipment WHERE Equipment_ID = ?",
                null, equipmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Equipment> fetch(String sql, String like, Integer id) {
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (like != null) {
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, like);
                }
            }
            if (id != null) {
                ps.setInt(1, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Equipment(
                            rs.getInt("Equipment_ID"),
                            rs.getString("Equipment_Name"),
                            rs.getString("Category"),
                            rs.getInt("Quantity"),
                            rs.getInt("Available_Quantity"),
                            rs.getString("Condition"),
                            rs.getDouble("Replacement_Cost")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch equipment: " + e.getMessage());
        }
        return list;
    }
}
