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
                        "Added equipment: " + name + " (Qty: " + qty + ")");
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
                        "Updated equipment ID " + equipmentId + " (Qty: " + qty + ", Condition: " + condition + ")");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update equipment: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteEquipment(int equipmentId, int managerId) {
        String sql = "DELETE FROM Equipment WHERE Equipment_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "EQUIPMENT_DELETED",
                        "Deleted equipment ID " + equipmentId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete equipment: " + e.getMessage());
        }
        return false;
    }

    public List<Equipment> getAllEquipment() {
        return query("SELECT Equipment_ID, Equipment_Name, Category, Quantity, Available_Quantity, Condition, "
                + "Replacement_Cost FROM Equipment ORDER BY Equipment_ID", null);
    }

    public List<Equipment> searchEquipment(String keyword) {
        return query("SELECT Equipment_ID, Equipment_Name, Category, Quantity, Available_Quantity, Condition, "
                + "Replacement_Cost FROM Equipment WHERE Equipment_Name LIKE ? OR Category LIKE ? OR Condition LIKE ? "
                + "ORDER BY Equipment_ID", keyword);
    }

    private List<Equipment> query(String sql, String keyword) {
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (keyword != null) {
                String like = "%" + keyword + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
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
