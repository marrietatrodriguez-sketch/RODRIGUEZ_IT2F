package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportDao {

    /** Returns a single-row summary used by the Equipment Inventory and Borrowing Report. */
    public String buildSummary() {
        String sql = "SELECT "
                + "(SELECT COALESCE(SUM(Quantity),0) FROM Equipment) AS total_equipment, "
                + "(SELECT COALESCE(SUM(Available_Quantity),0) FROM Equipment) AS available_equipment, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Return_Status = 'NOT_RETURNED' AND Request_Status = 'APPROVED') AS borrowed, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Request_Status = 'PENDING') AS pending_requests, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Return_Status = 'NOT_RETURNED' AND Due_Date < DATE('now')) AS overdue, "
                + "(SELECT COALESCE(SUM(Amount),0) FROM Penalties) AS total_penalties";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                sb.append(String.format("Total equipment : %d%n", rs.getInt("total_equipment")));
                sb.append(String.format("Available       : %d%n", rs.getInt("available_equipment")));
                sb.append(String.format("Borrowed        : %d%n", rs.getInt("borrowed")));
                sb.append(String.format("Pending requests: %d%n", rs.getInt("pending_requests")));
                sb.append(String.format("Overdue         : %d%n", rs.getInt("overdue")));
                sb.append(String.format("Total penalties : P%.2f%n", rs.getDouble("total_penalties")));
            }
        } catch (SQLException e) {
            System.err.println("Report failed: " + e.getMessage());
        }
        return sb.toString();
    }
}
