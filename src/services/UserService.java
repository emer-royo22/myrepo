package services;

import dbconnection.DBConnection;
import models.User;
import models.Customer;
import java.sql.*;

public class UserService {
    private Connection connection;

    public UserService() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    // Register new customer (stores only in customer table)
    public boolean registerCustomer(Customer customer) {
        try {
            connection.setAutoCommit(false);
            
            // 1. Check if username exists in either table
            if (usernameExists(customer.getUsername())) {
                return false;
            }

            // 2. Insert into customers table only
            String customerSql = "INSERT INTO customers (username, password, address, cellNo) VALUES (?, ?, ?, ?)";
            try (PreparedStatement customerStmt = connection.prepareStatement(customerSql)) {
                customerStmt.setString(1, customer.getUsername());
                customerStmt.setString(2, customer.getPassword());
                customerStmt.setString(3, customer.getAddress());
                customerStmt.setString(4, customer.getCellNo());
                customerStmt.executeUpdate();
            }

            connection.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Authenticate user by checking both tables
    public User authenticate(String username, String password) throws SQLException {
        // First check customers table
        String customerSql = "SELECT * FROM customers WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(customerSql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUsername(username);
                user.setRole("Customer");
                return user;
            }
        }

        // Then check users table (for staff/admin)
        String userSql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(userSql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUsername(username);
                user.setRole(rs.getString("role_id")); // "Staff" or "Admin"
                return user;
            }
        }
        
        return null;
    }

    // Check if username exists in either table
    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM (SELECT username FROM customers UNION SELECT username FROM users) AS all_users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // For admin to create staff accounts (stores in users table)
    public boolean createStaffAccount(User staffUser) throws SQLException {
        if (usernameExists(staffUser.getUsername())) {
            return false;
        }

        String sql = "INSERT INTO users (username, password, role_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, staffUser.getUsername());
            stmt.setString(2, staffUser.getPassword());
            stmt.setString(3, "Staff");
            return stmt.executeUpdate() > 0;
        }
    }
}