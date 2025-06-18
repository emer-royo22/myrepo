package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.User;
import models.Customer;
import services.UserService;

/*public class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("Authentication System");
        setSize(350, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title label
        JLabel titleLabel = new JLabel("Welcome", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        
        JButton staffCustomerLoginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Sign Up");
        JButton adminLoginBtn = new JButton("Admin Login");
        JButton exitBtn = new JButton("Exit");
        
        buttonPanel.add(staffCustomerLoginBtn);
        buttonPanel.add(adminLoginBtn);
        buttonPanel.add(signupBtn);
        buttonPanel.add(exitBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Action listeners
        staffCustomerLoginBtn.addActionListener(e -> showStaffCustomerLogin());
        adminLoginBtn.addActionListener(e -> showAdminLogin());
        signupBtn.addActionListener(this::showSignupForm);
        exitBtn.addActionListener(e -> System.exit(0));
    }

    private void showStaffCustomerLogin() {
        showLoginDialog("Login", false);
    }

    private void showAdminLogin() {
        showLoginDialog("Admin Login", true);
    }

    private void showLoginDialog(String title, boolean isAdminLogin) {
        JDialog loginDialog = new JDialog(this, title, true);
        loginDialog.setLayout(new BorderLayout());
        loginDialog.setSize(300, 200);
        loginDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            try {
                UserService userService = new UserService();
                User user = userService.authenticate(
                    userField.getText(), 
                    new String(passField.getPassword())
                );

                if (user != null) {
                    // Role validation
                    if (isAdminLogin && !"1".equals(user.getRole())) {
                        JOptionPane.showMessageDialog(loginDialog, 
                            "Only admin accounts can login here", 
                            "Access Denied", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    if (!isAdminLogin && "1".equals(user.getRole())) {
                        JOptionPane.showMessageDialog(loginDialog, 
                            "Admins must use Admin Login", 
                            "Access Denied", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JOptionPane.showMessageDialog(loginDialog, 
                        "Welcome " + user.getUsername() + " (" + user.getRole() + ")");
                    
                    // Redirect to appropriate dashboard
                    if ("Customer".equals(user.getRole())) {
                        new CustomerDashboardFrame(user).setVisible(true);
                    } else {
                        new DashboardFrame(user).setVisible(true);
                    }
                    
                    loginDialog.dispose();
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(loginDialog, 
                        "Invalid username or password", 
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                Logger.getLogger(LoginFrame.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(loginDialog, 
                    "Database error. Please try again later.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                Logger.getLogger(LoginFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }

    private void showSignupForm(ActionEvent e) {
        JDialog signupDialog = new JDialog(this, "Customer Registration", true);
        signupDialog.setLayout(new BorderLayout());
        signupDialog.setSize(400, 350);
        signupDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JTextField addressField = new JTextField();
        JTextField cellNoField = new JTextField();
        
        // Add input restriction for cell number (max 11 digits)
        cellNoField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (cellNoField.getText().length() >= 11) {
                    e.consume();
                }
                if (!Character.isDigit(e.getKeyChar())) {
                    e.consume();
                }
            }
        });
        
        JButton registerBtn = new JButton("Register");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);
        panel.add(new JLabel("Cell Number (11 digits):"));
        panel.add(cellNoField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(registerBtn);
        
        signupDialog.add(panel, BorderLayout.CENTER);
        signupDialog.add(buttonPanel, BorderLayout.SOUTH);

        registerBtn.addActionListener(ev -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            String address = addressField.getText().trim();
            String cellNo = cellNoField.getText().trim();

            // Validation
            if (username.isEmpty() || password.isEmpty() || address.isEmpty() || cellNo.isEmpty()) {
                JOptionPane.showMessageDialog(signupDialog, 
                    "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (cellNo.length() != 11) {
                JOptionPane.showMessageDialog(signupDialog,
                    "Cell number must be exactly 11 digits",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(signupDialog, 
                    "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Create customer object only (no user object)
                Customer newCustomer = new Customer();
                newCustomer.setUsername(username);
                newCustomer.setPassword(password);
                newCustomer.setAddress(address);
                newCustomer.setCellNo(cellNo);

                // Register the customer (only in customers table)
                UserService userService = new UserService();
                if (userService.registerCustomer(newCustomer)) {
                    JOptionPane.showMessageDialog(signupDialog, 
                        "Registration successful! Please login.", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    signupDialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(signupDialog, 
                        "Username already exists", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                Logger.getLogger(LoginFrame.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(signupDialog, 
                    "Database error. Please try again later.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        signupDialog.setVisible(true);
    }
}