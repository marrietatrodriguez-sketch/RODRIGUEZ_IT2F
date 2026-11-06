
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // REGISTER A NEW ACCOUNT
    public boolean registerUser(
            String username,
            String email,
            String password,
            String role) {

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

    // ADD AN ACCOUNT BY ADMIN
    public boolean addUser(
            String username,
            String email,
            String password,
            String role) {

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

    // RETRIEVE ALL USER ACCOUNTS
    public List<User> getAllUsers() {
        return getUsers(
                "SELECT id, username, email, password, roles, status " +
                        "FROM users ORDER BY id",
                null
        );
    }

    // RETRIEVE PENDING ACCOUNTS
    public List<User> getPendingUsers() {
        return getUsers(
                "SELECT id, username, email, password, roles, status " +
                        "FROM users WHERE status = ? ORDER BY id",
                "PENDING"
        );
    }

    // REUSABLE METHOD TO RETRIEVE USERS
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

    // APPROVE A PENDING ACCOUNT
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

    // UPDATE USERNAME, EMAIL, AND ROLE
    public boolean updateUser(
            int userId,
            String username,
            String email,
            String role) {

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

    // NEW: UPDATE USER ROLE AND STATUS
    public boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus) {

        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Updating user role and status failed: "
                    + e.getMessage());
            return false;
        }
    }

    // DEACTIVATE AN ACCOUNT
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

    // RETRIEVE USER BY USERNAME
    public User getUserByUsername(String username) {

        String sql = "SELECT id, username, email, password, roles, status " +
                "FROM users WHERE username = ?";

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
            System.err.println("Retrieving user by username failed: "
                    + e.getMessage());
        }

        return null;
    }
}
