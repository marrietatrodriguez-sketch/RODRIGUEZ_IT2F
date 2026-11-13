package com.scc.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportDao {

    /** Metrics for the Equipment Inventory and Borrowing Report, in display order. */
    public Map<String, String> metrics() {

        Map<String, String> m = new LinkedHashMap<>();

        String sql = "SELECT "
                + "(SELECT COALESCE(SUM(Quantity),0) FROM Equipment) AS total_equipment, "
                + "(SELECT COALESCE(SUM(Available_Quantity),0) FROM Equipment) AS available_equipment, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Request_Status = 'APPROVED' "
                + "AND Return_Status = 'NOT_RETURNED') AS borrowed, "
                + "(SELECT COUNT(*) FROM Penalties WHERE Penalty_Type = 'DAMAGED') AS damaged, "
                + "(SELECT COUNT(*) FROM Penalties WHERE Penalty_Type = 'MISSING') AS missing, "
                + "(SELECT COUNT(*) FROM Events WHERE Event_Date >= date('now')) AS upcoming_events, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Request_Status = 'PENDING') AS pending, "
                + "(SELECT COUNT(*) FROM Borrowing_Transactions WHERE Request_Status = 'APPROVED' "
                + "AND Return_Status = 'NOT_RETURNED' AND Due_Date < date('now')) AS overdue, "
                + "(SELECT COALESCE(SUM(Amount),0) FROM Penalties) AS total_penalties";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                m.put("Total equipment", String.valueOf(rs.getInt("total_equipment")));
                m.put("Available equipment", String.valueOf(rs.getInt("available_equipment")));
                m.put("Borrowed equipment", String.valueOf(rs.getInt("borrowed")));
                m.put("Damaged (records)", String.valueOf(rs.getInt("damaged")));
                m.put("Missing (records)", String.valueOf(rs.getInt("missing")));
                m.put("Upcoming events", String.valueOf(rs.getInt("upcoming_events")));
                m.put("Pending requests", String.valueOf(rs.getInt("pending")));
                m.put("Overdue equipment", String.valueOf(rs.getInt("overdue")));
                m.put("Total penalties", String.format("P%.2f", rs.getDouble("total_penalties")));
            }

        } catch (SQLException e) {
            System.err.println("Report failed: " + e.getMessage());
        }

        return m;
    }

    /** Writes the report as CSV (Metric,Value). Returns true on success. */
    public boolean exportCsv(String filePath) {

        StringBuilder sb = new StringBuilder("Metric,Value\n");
        metrics().forEach((k, v) -> sb.append(k).append(',').append(v).append('\n'));

        try {
            Files.writeString(Paths.get(filePath), sb.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
            return false;
        }
    }
}
