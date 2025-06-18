package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.User;
import services.DBService;

public class CustomerDashboardFrame extends JFrame {
    private final User user;
    private final DBService dbService;
    private JTable productTable;

    public CustomerDashboardFrame(User user) throws Exception {
        this.user = user;
        this.dbService = new DBService();

        setTitle("Customer Dashboard - " + user.getUsername());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        productTable = createProductTable();
        JScrollPane scrollPane = new JScrollPane(productTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel orderPanel = new JPanel(new FlowLayout());
        JButton orderBtn = new JButton("Place Order");
        JButton clearBtn = new JButton("Clear Selection");
        JButton logoutBtn = new JButton("Logout");

        orderBtn.addActionListener(e -> showOrderConfirmation());
        clearBtn.addActionListener(e -> clearSelections());
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        orderPanel.add(orderBtn);
        orderPanel.add(clearBtn);
        orderPanel.add(logoutBtn);
        mainPanel.add(orderPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JTable createProductTable() {
        try {
            String query = "SELECT id, name, price, stocks FROM products WHERE stocks > 0";
            ResultSet rs = dbService.executeQuery(query);
            DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "Name", "Price", "Available", "Order Quantity"}, 0
            ) {
                public boolean isCellEditable(int row, int column) {
                    return column == 4;
                }

                public Class<?> getColumnClass(int columnIndex) {
                    return (columnIndex == 4) ? Integer.class : super.getColumnClass(columnIndex);
                }
            };

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stocks"),
                    0
                });
            }

            JTable table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            return table;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage());
            return new JTable();
        }
    }

    private void clearSelections() {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(0, i, 4);
        }
        productTable.clearSelection();
    }

    private void showOrderConfirmation() {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        List<ProductOrder> orderItems = new ArrayList<>();
        double total = 0;
        boolean hasOrder = false;

        for (int i = 0; i < model.getRowCount(); i++) {
            int qty = (int) model.getValueAt(i, 4);
            if (qty > 0) {
                hasOrder = true;
                int productId = (int) model.getValueAt(i, 0);
                String name = (String) model.getValueAt(i, 1);
                double price = (double) model.getValueAt(i, 2);
                int stocks = (int) model.getValueAt(i, 3);

                if (qty > stocks) {
                    JOptionPane.showMessageDialog(this, 
                        "Not enough stock for " + name + "\nAvailable: " + stocks, 
                        "Stock Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double subtotal = qty * price;
                total += subtotal;
                orderItems.add(new ProductOrder(productId, name, qty, price, subtotal));
            }
        }

        if (!hasOrder) {
            JOptionPane.showMessageDialog(this, "Please select at least one product to order.");
            return;
        }

        showOrderSummary(orderItems, total);
    }

    private void showOrderSummary(List<ProductOrder> orderItems, double total) {
        JDialog summaryDialog = new JDialog(this, "Order Summary", true);
        summaryDialog.setLayout(new BorderLayout());
        summaryDialog.setSize(500, 400);
        summaryDialog.setLocationRelativeTo(this);

        DefaultTableModel summaryModel = new DefaultTableModel(
            new Object[]{"Product", "Quantity", "Unit Price", "Subtotal"}, 0
        );

        for (ProductOrder item : orderItems) {
            summaryModel.addRow(new Object[]{
                item.getName(),
                item.getQuantity(),
                String.format("₱%.2f", item.getPrice()),
                String.format("₱%.2f", item.getSubtotal())
            });
        }

        JTable summaryTable = new JTable(summaryModel);
        JScrollPane scrollPane = new JScrollPane(summaryTable);

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(new JLabel(String.format("Total Amount: ₱%.2f", total)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton confirmBtn = new JButton("Confirm Order");
        JButton cancelBtn = new JButton("Cancel");

        confirmBtn.addActionListener(e -> {
            showPaymentDialog(total);
            summaryDialog.dispose();
        });

        cancelBtn.addActionListener(e -> summaryDialog.dispose());

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);

        summaryDialog.add(scrollPane, BorderLayout.CENTER);
        summaryDialog.add(totalPanel, BorderLayout.NORTH);
        summaryDialog.add(buttonPanel, BorderLayout.SOUTH);

        summaryDialog.setVisible(true);
    }

   private void showPaymentDialog(double total) {
    JDialog paymentDialog = new JDialog(this, "Payment", true);
    paymentDialog.setLayout(new BorderLayout());
    paymentDialog.setSize(400, 200);
    paymentDialog.setLocationRelativeTo(this);

    JPanel centerPanel = new JPanel(new GridLayout(3, 1));
    centerPanel.add(new JLabel(String.format("Total Amount: ₱%.2f", total), SwingConstants.CENTER));
    centerPanel.add(new JLabel("Enter Payment Amount:", SwingConstants.CENTER));

    JTextField paymentField = new JTextField(10);
    JPanel fieldPanel = new JPanel();
    fieldPanel.add(paymentField);
    centerPanel.add(fieldPanel);

    JPanel buttonPanel = new JPanel();
    JButton payBtn = new JButton("Pay");
    JButton cancelBtn = new JButton("Cancel");

    payBtn.addActionListener(e -> {
        try {
            double payment = Double.parseDouble(paymentField.getText());
            if (payment < total) {
                JOptionPane.showMessageDialog(paymentDialog, 
                    "Insufficient payment! Please enter at least ₱" + total,
                    "Payment Error", JOptionPane.ERROR_MESSAGE);
            } else {
                if (processOrder()) {
                    JOptionPane.showMessageDialog(this, 
                        String.format("Order placed successfully!\nChange: ₱%.2f", (payment - total)),
                        "Order Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    paymentDialog.dispose();
                    
                    // Ask user if they want to continue ordering
                    int option = JOptionPane.showConfirmDialog(this,
                        "Order placed successfully! Would you like to continue shopping?",
                        "Continue Shopping?", JOptionPane.YES_NO_OPTION);
                    
                    if (option == JOptionPane.NO_OPTION) {
                        clearSelections();
                    }
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(paymentDialog, 
                "Please enter a valid payment amount", 
                "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    });

    cancelBtn.addActionListener(e -> paymentDialog.dispose());

    buttonPanel.add(payBtn);
    buttonPanel.add(cancelBtn);

    paymentDialog.add(centerPanel, BorderLayout.CENTER);
    paymentDialog.add(buttonPanel, BorderLayout.SOUTH);

    paymentDialog.setVisible(true);
}

private boolean processOrder() {
    DefaultTableModel model = (DefaultTableModel) productTable.getModel();
    Connection conn = null;

    try {
        conn = dbService.getConnection();
        conn.setAutoCommit(false);

        String updateStockSQL = "UPDATE products SET stocks = stocks - ? WHERE id = ?";
        String recordSaleSQL = "INSERT INTO sales (productId, quantity_sold, sale_date, customer_id) VALUES (?, ?, ?, ?)";
        String checkTotalSalesSQL = "SELECT amount FROM totalsales WHERE sale_date = ?";
        String updateTotalSalesSQL = "UPDATE totalsales SET amount = amount + ? WHERE sale_date = ?";
        String insertTotalSalesSQL = "INSERT INTO totalsales (sale_date, amount) VALUES (?, ?)";

        PreparedStatement updateStmt = conn.prepareStatement(updateStockSQL);
        PreparedStatement salesStmt = conn.prepareStatement(recordSaleSQL);

        double totalAmount = 0;
        Date today = new Date(System.currentTimeMillis());

        for (int i = 0; i < model.getRowCount(); i++) {
            int qty = (int) model.getValueAt(i, 4);
            if (qty > 0) {
                int productId = (int) model.getValueAt(i, 0);
                int currentStocks = (int) model.getValueAt(i, 3);
                double price = (double) model.getValueAt(i, 2);
                double subtotal = qty * price;

                // Update product stock
                updateStmt.setInt(1, qty);
                updateStmt.setInt(2, productId);
                updateStmt.addBatch();

                // Record individual sale with customer ID
                salesStmt.setInt(1, productId);
                salesStmt.setInt(2, qty);
                salesStmt.setDate(3, today);
                salesStmt.setInt(4, user.getId());
                salesStmt.addBatch();

                totalAmount += subtotal;
            }
        }

        updateStmt.executeBatch();
        salesStmt.executeBatch();

        updateStmt.close();
        salesStmt.close();

        // Handle total sales
        PreparedStatement checkTotalStmt = conn.prepareStatement(checkTotalSalesSQL);
        checkTotalStmt.setDate(1, today);
        ResultSet rs = checkTotalStmt.executeQuery();

        if (rs.next()) {
            PreparedStatement updateTotalStmt = conn.prepareStatement(updateTotalSalesSQL);
            updateTotalStmt.setDouble(1, totalAmount);
            updateTotalStmt.setDate(2, today);
            updateTotalStmt.executeUpdate();
            updateTotalStmt.close();
        } else {
            PreparedStatement insertTotalStmt = conn.prepareStatement(insertTotalSalesSQL);
            insertTotalStmt.setDate(1, today);
            insertTotalStmt.setDouble(2, totalAmount);
            insertTotalStmt.executeUpdate();
            insertTotalStmt.close();
        }

        checkTotalStmt.close();
        conn.commit();

        // Refresh product table to show updated stocks
        refreshProductTable();
        return true;

    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        }
        JOptionPane.showMessageDialog(this, 
            "Error processing order: " + e.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
        return false;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}



    private void refreshProductTable() {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        model.setRowCount(0); // Clear existing data
        
        try {
            String query = "SELECT id, name, price, stocks FROM products WHERE stocks > 0";
            ResultSet rs = dbService.executeQuery(query);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stocks"),
                    0
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to refresh products: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper class to store order items
    private static class ProductOrder {
        private final int productId;
        private final String name;
        private final int quantity;
        private final double price;
        private final double subtotal;

        public ProductOrder(int productId, String name, int quantity, double price, double subtotal) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        public int getProductId() { return productId; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getSubtotal() { return subtotal; }
    }
}