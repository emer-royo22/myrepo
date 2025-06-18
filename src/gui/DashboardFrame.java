package gui;

import models.User;
import services.DBService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DashboardFrame extends JFrame {
    private final User user;
    private final DBService dbService;
    private JTabbedPane tabbedPane;

    public DashboardFrame(User user) {
        this.user = user;

        try {
            this.dbService = new DBService();

            setTitle("Dashboard - " + user.getUsername() + " (" + user.getRole() + ")");
            setSize(800, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            initUI();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error initializing database service: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Failed to initialize DashboardFrame", e);
        }
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Products", createTablePanel("products"));
        tabbedPane.addTab("Suppliers", createTablePanel("suppliers"));

        if ("1".equals(user.getRole())) {
            tabbedPane.addTab("Users", createTablePanel("users"));
            tabbedPane.addTab("Sales", createTablePanel("sales"));
            tabbedPane.addTab("Customers", createTablePanel("customers"));
            tabbedPane.addTab("Total Sales", createTablePanel("totalsales"));
            tabbedPane.addTab("Admin Controls", createAdminControlsPanel());
        }

        add(tabbedPane);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private boolean canModifyTable(String tableName) {
        return "1".equals(user.getRole()) ||
                ("Staff".equals(user.getRole()) && ("products".equals(tableName) || "suppliers".equals(tableName)));
    }

    private JPanel createTablePanel(String tableName) {
        JPanel panel = new JPanel(new BorderLayout());

        try {
            ResultSet rs = dbService.getTableData(tableName);
            JTable table = new JTable(buildTableModel(rs));
            JScrollPane scrollPane = new JScrollPane(table);
            panel.add(scrollPane, BorderLayout.CENTER);

            if ("products".equals(tableName) || "sales".equals(tableName)) {
                JButton refreshBtn = new JButton("Refresh");
                refreshBtn.addActionListener(e -> refreshTable(tableName, table));
                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                topPanel.add(refreshBtn);
                panel.add(topPanel, BorderLayout.NORTH);
            }

            if (canModifyTable(tableName)) {
                addTableActionButtons(panel, tableName, table);
            }

        } catch (SQLException e) {
            handleTableLoadError(e);
        }

        return panel;
    }

    private void refreshTable(String tableName, JTable table) {
        try {
            ResultSet rs = dbService.getTableData(tableName);
            DefaultTableModel model = buildTableModel(rs);
            table.setModel(model);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error refreshing table: " + ex.getMessage(),
                    "Refresh Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTableActionButtons(JPanel panel, String tableName, JTable table) {
        JPanel buttonPanel = new JPanel();

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> showAddDialog(tableName));
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> showEditDialog(tableName, table));
        buttonPanel.add(editBtn);

        if ("products".equals(tableName)) {
            JButton addStockBtn = new JButton("Add Stock");
            addStockBtn.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Select a product to add stock.");
                    return;
                }

                int id = (int) table.getValueAt(selectedRow, 0);
                int currentStock = (int) table.getValueAt(selectedRow, 3); // column index 3 = stocks

                String input = JOptionPane.showInputDialog(this, "Enter stock quantity to add:");
                if (input != null && !input.trim().isEmpty()) {
                    try {
                        int addQty = Integer.parseInt(input);
                        int newStock = currentStock + addQty;

                        String sql = "UPDATE products SET stocks=? WHERE id=?";
                        PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                        stmt.setInt(1, newStock);
                        stmt.setInt(2, id);
                        stmt.executeUpdate();

                        // Refresh the table after update
                        refreshTable(tableName, table);
                        JOptionPane.showMessageDialog(this, "Stock updated. New stock: " + newStock);
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Invalid number format.");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
                    }
                }
            });
            buttonPanel.add(addStockBtn);
        }

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteRecord(tableName, table));
        buttonPanel.add(deleteBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handleTableLoadError(SQLException e) {
        JOptionPane.showMessageDialog(this,
                "Error loading table data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        Object[][] data = {};
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }

        return model;
    }

    private JPanel createAdminControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton backupBtn = new JButton("Backup Database");
        backupBtn.addActionListener(e -> {
            if (dbService.backupDatabase()) {
                JOptionPane.showMessageDialog(this, "Backup created successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Backup failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton restoreBtn = new JButton("Restore Database");
        restoreBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Restore functionality would be implemented here");
        });

        panel.add(backupBtn);
        panel.add(restoreBtn);
        return panel;
    }

    private void showAddDialog(String tableName) {
        if ("products".equals(tableName)) {
            JTextField name = new JTextField();
            JTextField price = new JTextField();
            JTextField stocks = new JTextField();
            JTextField supplierId = new JTextField();

            Object[] fields = {
                    "Name:", name,
                    "Price:", price,
                    "Stocks:", stocks,
                    "Supplier ID:", supplierId
            };

            int res = JOptionPane.showConfirmDialog(this, fields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    String sql = "INSERT INTO products (name, price, stocks, supplierId) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                    stmt.setString(1, name.getText());
                    stmt.setDouble(2, Double.parseDouble(price.getText()));
                    stmt.setInt(3, Integer.parseInt(stocks.getText()));
                    stmt.setInt(4, Integer.parseInt(supplierId.getText()));
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Product added.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        }

        if ("suppliers".equals(tableName)) {
            JTextField name = new JTextField();
            JTextField contact = new JTextField();

            Object[] fields = {
                    "Name:", name,
                    "Contact Info:", contact
            };

            int res = JOptionPane.showConfirmDialog(this, fields, "Add Supplier", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    String sql = "INSERT INTO suppliers (name, contact) VALUES (?, ?)";
                    PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                    stmt.setString(1, name.getText());
                    stmt.setString(2, contact.getText());
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Supplier added.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        }
    }

    private void showEditDialog(String tableName, JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record to edit.");
            return;
        }

        int id = (int) table.getValueAt(row, 0);

        if ("products".equals(tableName)) {
            JTextField name = new JTextField((String) table.getValueAt(row, 1));
            JTextField price = new JTextField(table.getValueAt(row, 2).toString());
            JTextField stocks = new JTextField(table.getValueAt(row, 3).toString());
            JTextField supplierId = new JTextField(table.getValueAt(row, 4).toString());

            Object[] fields = {
                    "Name:", name,
                    "Price:", price,
                    "Stocks:", stocks,
                    "Supplier ID:", supplierId
            };

            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    String sql = "UPDATE products SET name=?, price=?, stocks=?, supplierId=? WHERE id=?";
                    PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                    stmt.setString(1, name.getText());
                    stmt.setDouble(2, Double.parseDouble(price.getText()));
                    stmt.setInt(3, Integer.parseInt(stocks.getText()));
                    stmt.setInt(4, Integer.parseInt(supplierId.getText()));
                    stmt.setInt(5, id);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Product updated.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        }

        if ("suppliers".equals(tableName)) {
            JTextField name = new JTextField((String) table.getValueAt(row, 1));
            JTextField contact = new JTextField((String) table.getValueAt(row, 2));

            Object[] fields = {
                    "Name:", name,
                    "Contact Info:", contact
            };

            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Supplier", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    String sql = "UPDATE suppliers SET name=?, contact=? WHERE id=?";
                    PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                    stmt.setString(1, name.getText());
                    stmt.setString(2, contact.getText());
                    stmt.setInt(3, id);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Supplier updated.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        }
    }

    private void deleteRecord(String tableName, JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected record?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) table.getValueAt(row, 0);

            if (dbService.deleteRecord(tableName, id)) {
                ((DefaultTableModel) table.getModel()).removeRow(row);
                JOptionPane.showMessageDialog(this, "Record deleted.");
            } else {
                JOptionPane.showMessageDialog(this, "Delete failed.");
            }
        }
    }
}
