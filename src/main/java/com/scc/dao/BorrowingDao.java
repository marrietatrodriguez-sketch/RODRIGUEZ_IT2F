package com.scc.dao;

import com.scc.model.BorrowingTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BorrowingDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean createBorrowingRequest(int eventId, int userId, int equipmentId, int qty, String dueDate) {
        String sql = "INSERT INTO Borrowing_Transactions (Event_ID, User_ID, Equipment_ID, Quantity, Due_Date, "
                + "Request_Status, Return_Status) VALUES (?, ?, ?, ?, ?, 'PENDING', 'NOT_RETURNED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ps.setInt(3, equipmentId);
            ps.setInt(4, qty);
            ps.setString(5, dueDate);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(userId, "BORROWING_REQUESTED",
                        "Requested " + qty + " unit(s) of Equipment ID " + equipmentId + " for Event ID " + eventId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to submit borrowing request: " + e.getMessage());
        }
        return false;
    }

    public boolean updateRequestStatus(int borrowingId, String newStatus, int managerId) {
        String sql = "UPDATE Borrowing_Transactions SET Request_Status = ? WHERE Borrowing_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toUpperCase());
            ps.setInt(2, borrowingId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "BORROWING_" + newStatus.toUpperCase(),
                        "Manager set Borrowing ID " + borrowingId + " to " + newStatus.toUpperCase());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update request status: " + e.getMessage());
        }
        return false;
    }

    public boolean recordReturn(int borrowingId, String returnDate, int managerId) {
        String sql = "UPDATE Borrowing_Transactions SET Return_Date = ?, Return_Status = 'RETURNED' "
                + "WHERE Borrowing_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, returnDate);
            ps.setInt(2, borrowingId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(managerId, "EQUIPMENT_RETURNED",
                        "Recorded return for Borrowing ID " + borrowingId + " on " + returnDate);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to record return: " + e.getMessage());
        }
        return false;
    }

    public List<BorrowingTransaction> getAllTransactions() {
        return query("SELECT Borrowing_ID, Event_ID, User_ID, Equipment_ID, Quantity, Request_Date, Due_Date, "
                + "Return_Date, Request_Status, Return_Status FROM Borrowing_Transactions ORDER BY Borrowing_ID DESC", null);
    }

    public List<BorrowingTransaction> getTransactionsByUserId(int userId) {
        return query("SELECT Borrowing_ID, Event_ID, User_ID, Equipment_ID, Quantity, Request_Date, Due_Date, "
                + "Return_Date, Request_Status, Return_Status FROM Borrowing_Transactions "
                + "WHERE User_ID = ? ORDER BY Borrowing_ID DESC", userId);
    }

    public List<BorrowingTransaction> getPendingRequests() {
        return query("SELECT Borrowing_ID, Event_ID, User_ID, Equipment_ID, Quantity, Request_Date, Due_Date, "
                + "Return_Date, Request_Status, Return_Status FROM Borrowing_Transactions "
                + "WHERE Request_Status = 'PENDING' ORDER BY Borrowing_ID", null);
    }

    private List<BorrowingTransaction> query(String sql, Integer userId) {
        List<BorrowingTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BorrowingTransaction(
                            rs.getInt("Borrowing_ID"),
                            rs.getInt("Event_ID"),
                            rs.getInt("User_ID"),
                            rs.getInt("Equipment_ID"),
                            rs.getInt("Quantity"),
                            rs.getString("Request_Date"),
                            rs.getString("Due_Date"),
                            rs.getString("Return_Date"),
                            rs.getString("Request_Status"),
                            rs.getString("Return_Status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch borrowing transactions: " + e.getMessage());
        }
        return list;
    }
}
