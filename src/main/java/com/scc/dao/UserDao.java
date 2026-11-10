
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    // REGISTER USER
    public boolean registerUser(String username, String email, String password, String role) {

        if (role == null ||
                !List.of("USER", "MANAGER", "ADMIN").contains(role.toUpperCase())) {

            System.out.println("Registration failed: invalid account role.");
            return false;
        }

        String selectedRole = role.toUpperCase();

        String sql = "INSERT INTO Users "
                + "(Username, Email, Password, Role, Status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, selectedRole);

            if (ps.executeUpdate() > 0) {

                int newId = getUserIdByUsername(username);

                activityLogDao.logAction(
                        newId,
                        "ACCOUNT_REGISTERED",
                        "User " + username + " registered with role "
                                + selectedRole + ". Status: PENDING."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
        }

        return false;
    }

    // ADD USER BY ADMIN
    public boolean addUser(String username, String email, String password,
                           String role, int adminId) {

        String sql = "INSERT INTO Users "
                + "(Username, Email, Password, Role, Status) "
                + "VALUES (?, ?, ?, ?, 'APPROVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role.toUpperCase());

            if (ps.executeUpdate() > 0) {

                int newId = getUserIdByUsername(username);

                activityLogDao.logAction(
                        adminId,
                        "USER_ADDED",
                        "Admin " + adminId + " added account for User ID "
                                + newId + " (" + username + ")."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Adding user failed: " + e.getMessage());
        }

        return false;
    }

    // GET ALL USERS
    public List<User> getAllUsers() {

        return queryUsers(
                "SELECT User_ID, Username, Email, Password, Role, Status "
                        + "FROM Users ORDER BY User_ID",
                null
        );
    }

    // GET PENDING USERS
    public List<User> getPendingUsers() {

        return queryUsers(
                "SELECT User_ID, Username, Email, Password, Role, Status "
                        + "FROM Users WHERE Status = ? ORDER BY User_ID",
                "PENDING"
        );
    }

    // GET USER BY USERNAME
    public User getUserByUsername(String username) {

        List<User> list = queryUsers(
                "SELECT User_ID, Username, Email, Password, Role, Status "
                        + "FROM Users WHERE Username = ?",
                username
        );

        return list.isEmpty() ? null : list.get(0);
    }

    // APPROVE USER
    public boolean approveUser(int userId, int adminId) {

        String sql = "UPDATE Users SET Status = 'APPROVED' "
                + "WHERE User_ID = ? AND Status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logAction(
                        adminId,
                        "USER_APPROVED",
                        "Admin " + adminId + " approved account for User ID "
                                + userId + "."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Approval failed: " + e.getMessage());
        }

        return false;
    }

    // UPDATE USER ROLE AND STATUS
    public boolean updateUserRoleAndStatus(int userId, String newRole,
                                           String newStatus, int adminId) {

        String sql = "UPDATE Users SET Role = ?, Status = ? "
                + "WHERE User_ID = ? AND Status != 'DEACTIVATED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logAction(
                        adminId,
                        "USER_UPDATED",
                        "Admin " + adminId + " changed User " + userId
                                + " to role=" + newRole.toUpperCase()
                                + ", status=" + newStatus.toUpperCase() + "."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
        }

        return false;
    }

    // DEACTIVATE USER
    public boolean deactivateUser(int userId, int adminId) {

        String sql = "UPDATE Users SET Status = 'DEACTIVATED' "
                + "WHERE User_ID = ? AND Status != 'DEACTIVATED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logAction(
                        adminId,
                        "USER_DEACTIVATED",
                        "Admin " + adminId + " deactivated User ID "
                                + userId + "."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Deactivation failed: " + e.getMessage());
        }

        return false;
    }

    // UPDATE PROFILE
    public boolean updateProfile(int userId, String email) {

        String sql = "UPDATE Users SET Email = ? WHERE User_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setInt(2, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logAction(
                        userId,
                        "PROFILE_UPDATED",
                        "User " + userId + " updated profile email."
                );

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Profile update failed: " + e.getMessage());
        }

        return false;
    }

    // QUERY USERS
    private List<User> queryUsers(String sql, String param) {

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (param != null) {
                ps.setString(1, param);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    users.add(new User(
                            rs.getInt("User_ID"),
                            rs.getString("Username"),
                            rs.getString("Email"),
                            rs.getString("Password"),
                            rs.getString("Role"),
                            rs.getString("Status")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Retrieving users failed: " + e.getMessage());
        }

        return users;
    }

    // GET USER ID BY USERNAME
    private int getUserIdByUsername(String username) {

        String sql = "SELECT User_ID FROM Users WHERE Username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("User_ID");
                }
            }

        } catch (SQLException e) {
            System.err.println("ID lookup failed: " + e.getMessage());
        }

        return -1;
    }
}
