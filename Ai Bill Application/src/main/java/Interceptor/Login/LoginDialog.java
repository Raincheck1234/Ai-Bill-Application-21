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

        setTitle("User Login"); // "用户登录"
        setLayout(new GridLayout(3, 2));
        setModal(true);
        setSize(300, 150);
        setResizable(false); // Prevent resizing

        // Input components
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        // Add components
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
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
                        "Incorrect username or password!", // "Wrong username or password!"
                        "Login Failed",                    // "Login Failed"
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