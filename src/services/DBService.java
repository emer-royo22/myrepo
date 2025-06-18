package services;

import dbconnection.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBService {
    private Connection connection;

    public DBService() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DBConnection.getConnection();
        }
        return this.connection;
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Statement statement = getConnection().createStatement();
        return statement.executeQuery(query);
    }

    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement.executeQuery();
    }

    public int executeUpdate(String query, Object... params) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        }
    }

    public ResultSet getTableData(String tableName) throws SQLException {
        String query = "SELECT * FROM " + tableName;
        return executeQuery(query);
    }

    public boolean deleteRecord(String tableName, int id) {
        try {
            String query = "DELETE FROM " + tableName + " WHERE id = ?";
            return executeUpdate(query, id) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean testConnection() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean recordSale(int productId, int quantitySold, int customerId) {
        try {
            String query = "INSERT INTO sales (productId, quantity_sold, sale_date, customer_id) VALUES (?, ?, ?, ?)";
            return executeUpdate(query, productId, quantitySold, new Date(System.currentTimeMillis()), customerId) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProductStock(int productId, int quantityChange) {
        try {
            String query = "UPDATE products SET stocks = stocks + ? WHERE id = ?";
            return executeUpdate(query, -quantityChange, productId) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTotalSales(Date date, double amount) {
        try {
            // Check if record exists
            String checkQuery = "SELECT 1 FROM totalsales WHERE sale_date = ?";
            ResultSet rs = executeQuery(checkQuery, date);
            
            if (rs.next()) {
                // Update existing record
                String updateQuery = "UPDATE totalsales SET amount = amount + ? WHERE sale_date = ?";
                return executeUpdate(updateQuery, amount, date) > 0;
            } else {
                // Insert new record
                String insertQuery = "INSERT INTO totalsales (sale_date, amount) VALUES (?, ?)";
                return executeUpdate(insertQuery, date, amount) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ProductInfo> getAvailableProducts() throws SQLException {
        List<ProductInfo> products = new ArrayList<>();
        String query = "SELECT id, name, price, stocks FROM products WHERE stocks > 0";
        try (ResultSet rs = executeQuery(query)) {
            while (rs.next()) {
                products.add(new ProductInfo(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stocks")
                ));
            }
        }
        return products;
    }

    public List<OrderHistory> getCustomerOrderHistory(int customerId) throws SQLException {
        List<OrderHistory> history = new ArrayList<>();
        String query = "SELECT s.id, p.name, s.quantity_sold, p.price, " +
                      "(s.quantity_sold * p.price) as total, s.sale_date " +
                      "FROM sales s JOIN products p ON s.productId = p.id " +
                      "WHERE s.customer_id = ? ORDER BY s.sale_date DESC";
        try (ResultSet rs = executeQuery(query, customerId)) {
            while (rs.next()) {
                history.add(new OrderHistory(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("quantity_sold"),
                    rs.getDouble("price"),
                    rs.getDouble("total"),
                    rs.getDate("sale_date")
                ));
            }
        }
        return history;
    }

    public boolean backupDatabase() {
        // Implementation would depend on your database system
        try {
            // Actual backup implementation would go here
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper classes for better data structure
    public static class ProductInfo {
        public final int id;
        public final String name;
        public final double price;
        public final int stock;

        public ProductInfo(int id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }
    }

    public static class OrderHistory {
        public final int id;
        public final String productName;
        public final int quantity;
        public final double unitPrice;
        public final double total;
        public final Date saleDate;

        public OrderHistory(int id, String productName, int quantity, double unitPrice, double total, Date saleDate) {
            this.id = id;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = total;
            this.saleDate = saleDate;
        }
    }
}