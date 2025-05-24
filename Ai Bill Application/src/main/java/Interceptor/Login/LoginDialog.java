package Interceptor.Login;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.concurrent.ExecutorService; // Import ExecutorService


public class LoginDialog extends JDialog {
    private User authenticatedUser = null;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final UserService userService;
    private final ExecutorService executorService; // NEW: ExecutorService field

    /**
     * Constructor now accepts UserService and ExecutorService.
     * @param userService The UserService instance for authentication and registration.
     * @param executorService Executor service for background tasks.
     */
    public LoginDialog(UserService userService, ExecutorService executorService) { // Accept ExecutorService
        this.userService = userService;
        this.executorService = executorService; // Assign ExecutorService

        setTitle("User Login");
        // Use BorderLayout for the main dialog content
        setLayout(new BorderLayout(10, 10));
        setModal(true);
        setSize(300, 200);
        setResizable(false);

        // --- Input Panel (Center) ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        add(inputPanel, BorderLayout.CENTER);


        // --- Button Panel (South) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton); // Register button
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);


        // Login button logic (same as before)
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            authenticatedUser = userService.authenticate(username, password);

            if (authenticatedUser != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid username or password!",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                clearFields();
            }
        });

        // Register button logic - Pass ExecutorService to RegistrationDialog
        registerButton.addActionListener(e -> {
            // Create and show the Registration dialog, pass ExecutorService
            RegistrationDialog registrationDialog = new RegistrationDialog(this, userService, executorService); // Pass ExecutorService
            registrationDialog.setVisible(true);
            clearFields(); // Clear login fields after clicking register
        });


        // Cancel button logic (same as before)
        cancelButton.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
            System.exit(0);
        });

        pack();
        setLocationRelativeTo(null);
    }

    // Method to clear input fields (same as before)
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }

    /**
     * Shows the login dialog and returns the authenticated user upon successful login.
     * Blocking call.
     * @return The authenticated User object, or null if login failed or was cancelled.
     */
    public User showDialogAndGetResult() {
        setVisible(true);
        return authenticatedUser;
    }
}