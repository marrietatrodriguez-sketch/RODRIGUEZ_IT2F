package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDao {

    public boolean registerUser(String username, String email, String password, String role){

        String sql = "INSERT INTO users(username, email, password, roles, status) VALUES(?, ?, ?, ?, 'PENDING')";

        try(Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("-> Succesful Registration!, Waiting for Approval");
            return true;
        } catch (SQLException e) {
            System.err.println("Failed: "+ e.getMessage());
            return false;
        }

    }
}
