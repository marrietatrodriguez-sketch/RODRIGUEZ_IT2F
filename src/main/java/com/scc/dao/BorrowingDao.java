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

    private static final String COLUMNS =
            "Borrowing_ID, Event_ID, User_ID, Equipment_ID, Quantity, Request_Date, Due_Date, "
                    + "Return_Date, Request_Status, Return_Status";

    // SUBMIT REQUEST (user). The event must belong to this user and enough units must be available.
    public boolean createBorrowingRequest(int eventId, int userId, int equipmentId, int qty, String dueDate) {

        String ownsEvent = "SELECT 1 FROM Events WHERE Event_ID = ? AND User_ID = ?";
        String stock = "SELECT Available_Quantity FROM Equipment WHERE Equipment_ID = ?";
        String insert = "INSERT INTO Borrowing_Transactions (Event_ID, User_ID, Equipment_ID, Quantity, "
                + "Due_Date, Request_Status, Return_Status) VALUES (?, ?, ?, ?, ?, 'PENDING', 'NOT_RETURNED')";

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(ownsEvent)) {
                ps.setInt(1, eventId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            int available = -1;
            try (PreparedStatement ps = conn.prepareStatement(stock)) {
                ps.setInt(1, equipmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        available = rs.getInt(1);
                    }
                }
            }

            if (available < qty) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, eventId);
                ps.setInt(2, userId);
                ps.setInt(3, equipmentId);
                ps.setInt(4, qty);
                ps.setString(5, dueDate);

                if (ps.executeUpdate() > 0) {
                    activityLogDao.logAction(userId, "BORROWING_REQUESTED",
                            "User " + userId + " requested " + qty + " unit(s) of Equipment ID "
                                    + equipmentId + " for Event ID " + eventId + ".");
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to submit borrowing request: " + e.getMessage());
        }
        return false;
    }

    // APPROVE (manager). Reserves the units in Available_Quantity. Returns null on success, else the reason.
    public String approveRequest(int borrowingId, int managerId) {

        String select = "SELECT Equipment_ID, Quantity, Request_Status FROM Borrowing_Transactions WHERE Borrowing_ID = ?";
        String stock = "SELECT Available_Quantity FROM Equipment WHERE Equipment_ID = ?";
        String reserve = "UPDATE Equipment SET Available_Quantity = Available_Quantity - ? WHERE Equipment_ID = ?";
        String approve = "UPDATE Borrowing_Transactions SET Request_Status = 'APPROVED' WHERE Borrowing_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int equipmentId;
                int qty;
                String status;

                try (PreparedStatement ps = conn.prepareStatement(select)) {
                    ps.setInt(1, borrowingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return "No borrowing request found with that ID.";
                        }
                        equipmentId = rs.getInt(1);
                        qty = rs.getInt(2);
                        status = rs.getString(3);
                    }
                }

                if (!"PENDING".equals(status)) {
                    conn.rollback();
                    return "Only PENDING requests can be approved (current status: " + status + ").";
                }

                int available = 0;
                try (PreparedStatement ps = conn.prepareStatement(stock)) {
                    ps.setInt(1, equipmentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            available = rs.getInt(1);
                        }
                    }
                }

                if (available < qty) {
                    conn.rollback();
                    return "Not enough available units (" + available + " available, " + qty + " requested).";
                }

                try (PreparedStatement ps = conn.prepareStatement(reserve)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, equipmentId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(approve)) {
                    ps.setInt(1, borrowingId);
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            activityLogDao.logAction(managerId, "BORROWING_APPROVED",
                    "Manager " + managerId + " approved Borrowing ID " + borrowingId + ".");
            return null;

        } catch (SQLException e) {
            System.err.println("Approval failed: " + e.getMessage());
            return "Database error while approving: " + e.getMessage();
        }
    }

    // REJECT (manager). Returns null on success, else the reason.
    public String rejectRequest(int borrowingId, int managerId) {

        String sql = "UPDATE Borrowing_Transactions SET Request_Status = 'REJECTED' "
                + "WHERE Borrowing_ID = ? AND Request_Status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowingId);
            if (ps.executeUpdate() == 0) {
                return "No PENDING request found with that ID.";
            }
        } catch (SQLException e) {
            System.err.println("Rejection failed: " + e.getMessage());
            return "Database error while rejecting: " + e.getMessage();
        }

        activityLogDao.logAction(managerId, "BORROWING_REJECTED",
                "Manager " + managerId + " rejected Borrowing ID " + borrowingId + ".");
        return null;
    }

    // RETURN (manager). Units go back to stock minus any missing units, which are removed from the total.
    // Returns null on success, else the reason.
    public String recordReturn(int borrowingId, String returnDate, int missingQty, int managerId) {

        String select = "SELECT Equipment_ID, Quantity, Request_Status, Return_Status "
                + "FROM Borrowing_Transactions WHERE Borrowing_ID = ?";
        String close = "UPDATE Borrowing_Transactions SET Return_Date = ?, Return_Status = 'RETURNED', "
                + "Request_Status = 'COMPLETED' WHERE Borrowing_ID = ?";
        String restock = "UPDATE Equipment SET Quantity = Quantity - ?, "
                + "Available_Quantity = Available_Quantity + ? WHERE Equipment_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int equipmentId;
                int qty;
                String requestStatus;
                String returnStatus;

                try (PreparedStatement ps = conn.prepareStatement(select)) {
                    ps.setInt(1, borrowingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return "No borrowing record found with that ID.";
                        }
                        equipmentId = rs.getInt(1);
                        qty = rs.getInt(2);
                        requestStatus = rs.getString(3);
                        returnStatus = rs.getString(4);
                    }
                }

                if (!"APPROVED".equals(requestStatus) || !"NOT_RETURNED".equals(returnStatus)) {
                    conn.rollback();
                    return "Only approved, unreturned borrowings can be returned (current: "
                            + requestStatus + " / " + returnStatus + ").";
                }

                if (missingQty < 0 || missingQty > qty) {
                    conn.rollback();
                    return "Missing units must be between 0 and " + qty + ".";
                }

                try (PreparedStatement ps = conn.prepareStatement(close)) {
                    ps.setString(1, returnDate);
                    ps.setInt(2, borrowingId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(restock)) {
                    ps.setInt(1, missingQty);
                    ps.setInt(2, qty - missingQty);
                    ps.setInt(3, equipmentId);
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            activityLogDao.logAction(managerId, "EQUIPMENT_RETURNED",
                    "Manager " + managerId + " recorded return for Borrowing ID " + borrowingId
                            + " on " + returnDate + " (missing: " + missingQty + " unit(s)).");
            return null;

        } catch (SQLException e) {
            System.err.println("Failed to record return: " + e.getMessage());
            return "Database error while recording return: " + e.getMessage();
        }
    }

    public BorrowingTransaction findById(int borrowingId) {
        List<BorrowingTransaction> list = fetch(
                "SELECT " + COLUMNS + " FROM Borrowing_Transactions WHERE Borrowing_ID = ?", borrowingId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<BorrowingTransaction> getAllTransactions() {
        return fetch("SELECT " + COLUMNS + " FROM Borrowing_Transactions ORDER BY Borrowing_ID DESC", null);
    }

    public List<BorrowingTransaction> getTransactionsByUserId(int userId) {
        return fetch("SELECT " + COLUMNS + " FROM Borrowing_Transactions WHERE User_ID = ? "
                + "ORDER BY Borrowing_ID DESC", userId);
    }

    public List<BorrowingTransaction> getPendingRequests() {
        return fetch("SELECT " + COLUMNS + " FROM Borrowing_Transactions WHERE Request_Status = 'PENDING' "
                + "ORDER BY Borrowing_ID", null);
    }

    // Approved and not yet returned, past the due date
    public List<BorrowingTransaction> getOverdueTransactions() {
        return fetch("SELECT " + COLUMNS + " FROM Borrowing_Transactions WHERE Request_Status = 'APPROVED' "
                + "AND Return_Status = 'NOT_RETURNED' AND Due_Date < date('now') ORDER BY Due_Date", null);
    }

    private List<BorrowingTransaction> fetch(String sql, Integer intParam) {
        List<BorrowingTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (intParam != null) {
                ps.setInt(1, intParam);
            }
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
