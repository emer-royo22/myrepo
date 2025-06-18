package gui;

import services.DBService;

import javax.swing.*;
import java.awt.*;
import java.sql.PreparedStatement;

public class AddStockFrame extends JFrame {
    public AddStockFrame(int productId, int currentStock, DBService dbService) {
        setTitle("Add Stock");
        setSize(300, 150);
        setLocationRelativeTo(null);

        JTextField addStockField = new JTextField(10);
        JButton updateBtn = new JButton("Add Stock");

        updateBtn.addActionListener(e -> {
            try {
                int addAmount = Integer.parseInt(addStockField.getText());
                if (addAmount < 0) throw new NumberFormatException("Negative not allowed");
                int newStock = currentStock + addAmount;

                String sql = "UPDATE products SET stocks = ? WHERE id = ?";
                PreparedStatement stmt = dbService.getConnection().prepareStatement(sql);
                stmt.setInt(1, newStock);
                stmt.setInt(2, productId);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Stock updated to " + newStock);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel panel = new JPanel();
        panel.add(new JLabel("Add Amount:"));
        panel.add(addStockField);
        panel.add(updateBtn);
        add(panel);
    }
}

    

