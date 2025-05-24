package Interceptor.Login;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    private User authenticatedUser = null;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final UserService userService;

    /**
     * Constructor now accepts UserService.
     * @param userService The UserService instance for authentication.
     */
    public LoginDialog(UserService userService) {
        this.userService = userService;

        setTitle("User Login");
        // Use BorderLayout for the main dialog content
        setLayout(new BorderLayout(10, 10)); // Add some padding
        setModal(true);
        setSize(300, 200); // Adjusted size for better fit
        setResizable(false);

        // --- Input Panel (Center) ---
        // Use GridLayout for username and password fields
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10)); // 2 rows, 2 columns with gaps
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10)); // Padding on top/sides

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        add(inputPanel, BorderLayout.CENTER); // Add input panel to the center of the dialog


        // --- Button Panel (South) ---
        // Use FlowLayout to center or align buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); // Align buttons to the right with gaps

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JButton cancelButton = new JButton("Cancel");

        // Add buttons to the panel
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton); // Register button
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH); // Add button panel to the south of the dialog


        // Login button logic (same as before)
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            authenticatedUser = userService.authenticate(username, password);

            if (authenticatedUser != null) {
                dispose(); // Close dialog
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid username or password!",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                clearFields(); // Clear fields on failure
            }
        });

        // Register button logic (same as before)
        registerButton.addActionListener(e -> {
            RegistrationDialog registrationDialog = new RegistrationDialog(this, userService);
            registrationDialog.setVisible(true);
            clearFields(); // Clear login fields after clicking register
        });


        // Cancel button logic (same as before)
        cancelButton.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
            System.exit(0);
        });

        // Pack the dialog to fit content and center it
        pack(); // Adjust size based on preferred sizes of components
        setLocationRelativeTo(null); // Center dialog on screen
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