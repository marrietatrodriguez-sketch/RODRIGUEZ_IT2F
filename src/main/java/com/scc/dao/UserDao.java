package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // Register a new account that requires approval
    public boolean registerUser(
            String username, String email,
            String password, String role) {

        String sql = "INSERT INTO users " +
                "(username, email, password, roles, status) " +
                "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role.toUpperCase());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    // Add an account directly as an administrator
    public boolean addUser(
            String username, String email,
            String password, String role) {

        String sql = "INSERT INTO users " +
                "(username, email, password, roles, status) " +
                "VALUES (?, ?, ?, ?, 'APPROVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role.toUpperCase());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Adding user failed: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all accounts
    public List<User> getAllUsers() {
        return getUsers(
                "SELECT id, username, email, password, roles, status " +
                        "FROM users ORDER BY id",
                null
        );
    }

    // Retrieve pending accounts of every role
    public List<User> getPendingUsers() {
        return getUsers(
                "SELECT id, username, email, password, roles, status " +
                        "FROM users WHERE status = ? ORDER BY id",
                "PENDING"
        );
    }

    private List<User> getUsers(String sql, String status) {
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (status != null) {
                ps.setString(1, status);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Retrieving users failed: " + e.getMessage());
        }

        return users;
    }

    // Approve a pending account
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' " +
                "WHERE id = ? AND status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Approval failed: " + e.getMessage());
            return false;
        }
    }

    // Update username, email, and role
    public boolean updateUser(
            int userId, String username,
            String email, String role) {

        String sql = "UPDATE users SET username = ?, " +
                "email = ?, roles = ? WHERE id = ? " +
                "AND status != 'DEACTIVATED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, role.toUpperCase());
            ps.setInt(4, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Updating user failed: " + e.getMessage());
            return false;
        }
    }

    // Deactivate an account without deleting its history
    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET status = 'DEACTIVATED' " +
                "WHERE id = ? AND status != 'DEACTIVATED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Deactivation failed: " + e.getMessage());
            return false;
        }
    }

    // Retrieve user by username for authentication
    public User getUserByUsername(String username) {
        String sql = "SELECT id, username, email, password, roles, status FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Retrieving user by username failed: " + e.getMessage());
        }

        return null;
    }
}