package com.scc.dao;

import com.scc.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean createEvent(int userId, String name, String date, String location, int guests) {
        String sql = "INSERT INTO Events (User_ID, Event_Name, Event_Date, Location, Number_of_Guests) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, date);
            ps.setString(4, location);
            ps.setInt(5, guests);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(userId, "EVENT_CREATED",
                        "Created event '" + name + "' on " + date + " at " + location);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to create event: " + e.getMessage());
        }
        return false;
    }

    public boolean updateEvent(int eventId, int userId, String name, String date, String location, int guests) {
        String sql = "UPDATE Events SET Event_Name = ?, Event_Date = ?, Location = ?, Number_of_Guests = ? "
                + "WHERE Event_ID = ? AND User_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, date);
            ps.setString(3, location);
            ps.setInt(4, guests);
            ps.setInt(5, eventId);
            ps.setInt(6, userId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(userId, "EVENT_MODIFIED", "Modified event ID " + eventId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update event: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteEvent(int eventId, int userId) {
        String sql = "DELETE FROM Events WHERE Event_ID = ? AND User_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(userId, "EVENT_DELETED", "Deleted event ID " + eventId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete event: " + e.getMessage());
        }
        return false;
    }

    public List<Event> getEventsByUserId(int userId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT Event_ID, User_ID, Event_Name, Event_Date, Location, Number_of_Guests "
                + "FROM Events WHERE User_ID = ? ORDER BY Event_ID DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new Event(
                            rs.getInt("Event_ID"),
                            rs.getInt("User_ID"),
                            rs.getString("Event_Name"),
                            rs.getString("Event_Date"),
                            rs.getString("Location"),
                            rs.getInt("Number_of_Guests")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch events: " + e.getMessage());
        }
        return events;
    }
}
