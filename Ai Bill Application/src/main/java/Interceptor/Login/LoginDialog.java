package Interceptor.Login;

import Service.User.UserService; // Import the new UserService
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    private User authenticatedUser = null; // Change from boolean flag to User object
    private JTextField usernameField;
    private JPasswordField passwordField;
    // UserService should ideally be injected, but for simplicity in LoginDialog,
    // we might initialize it here or pass it from Main. Let's pass it from Main.
    private final UserService userService;

    // Constructor now accepts UserService
    public LoginDialog(UserService userService) {
        this.userService = userService; // Inject UserService

        setTitle("用户登录");
        setLayout(new GridLayout(3, 2));
        setModal(true);
        setSize(300, 150);
        setResizable(false); // Prevent resizing

        // Input components
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginButton = new JButton("登录");
        JButton cancelButton = new JButton("取消");

        // Add components
        add(new JLabel("用户名:"));
        add(usernameField);
        add(new JLabel("密码:"));
        add(passwordField);
        add(loginButton);
        add(cancelButton);

        // Login button logic
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            authenticatedUser = userService.authenticate(username, password); // Use new authenticate method

            if (authenticatedUser != null) { // Check if a User object was returned
                dispose(); // Close dialog
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "用户名或密码错误！",
                        "登录失败",
                        JOptionPane.ERROR_MESSAGE
                );
                clearFields(); // Clear fields on failure
            }
        });

        // Cancel button logic
        cancelButton.addActionListener(e -> {
            authenticatedUser = null; // Ensure no user is set on cancel
            dispose();
            System.exit(0); // Exit application on cancel
        });

        setLocationRelativeTo(null); // Center dialog
    }

    // Method to clear input fields
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow(); // Focus back to username field
    }

    /**
     * Shows the login dialog and returns the authenticated user upon successful login.
     * Blocking call.
     * @return The authenticated User object, or null if login failed or was cancelled.
     */
    public User showDialogAndGetResult() {
        setVisible(true); // Show the dialog (this call is blocking because modal is true)
        return authenticatedUser; // Return the result after dialog is closed
    }

    // Remove isLoginSuccessful() as we now return the User object
    // public boolean isLoginSuccessful() { return loginSuccessful; }
}