package Interceptor.Login;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;

// Import the RegistrationDialog if it's in the same package or needs to be imported
// import Interceptor.Login.RegistrationDialog; // If RegistrationDialog is in this package
import com.group21.ai.Main;

public class LoginDialog extends JDialog {
    private ExecutorService executorService = Main.getExecutorService(); // ExecutorService for background tasks
    private User authenticatedUser = null; // Stores the authenticated User object
    private JTextField usernameField;
    private JPasswordField passwordField;

    private final UserService userService; // Injected UserService

    /**
     * Constructor for the Login Dialog.
     *
     * @param userService The UserService instance for authentication.
     */
    public LoginDialog(UserService userService) {
        this.userService = userService; // Assign injected service

        setTitle("User Login"); // Dialog title in English
        // Use BorderLayout for the main dialog layout for better structure
        setLayout(new BorderLayout(10, 10)); // Add gaps between areas
        setModal(true); // Make the dialog modal (blocks interaction with the parent window)
        setSize(300, 180); // Adjusted size for better fit
        setResizable(false); // Prevent resizing

        // --- Input Panel (Center) ---
        // Use GridLayout for username and password fields (Labels and fields in a 2x2 grid)
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10)); // 2 rows, 2 columns, with horizontal and vertical gaps
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10)); // Add padding on top and sides

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        inputPanel.add(new JLabel("Username:")); // Label for username
        inputPanel.add(usernameField);           // Username input field
        inputPanel.add(new JLabel("Password:")); // Label for password
        inputPanel.add(passwordField);           // Password input field

        add(inputPanel, BorderLayout.CENTER); // Add the input panel to the center of the dialog


        // --- Button Panel (South) ---
        // Use FlowLayout to arrange buttons (Login, Register, Cancel)
        // Align to the right for a common dialog button layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); // Align buttons to the right with gaps

        JButton loginButton = new JButton("Login");     // Login button
        JButton registerButton = new JButton("Register"); // Register button
        JButton cancelButton = new JButton("Cancel");   // Cancel button

        // Add buttons to the button panel
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton); // Add the Register button
        buttonPanel.add(cancelButton);   // Add the Cancel button

        add(buttonPanel, BorderLayout.SOUTH); // Add the button panel to the south of the dialog


        // --- Add Action Listeners for Buttons ---

        // Login button logic
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim(); // Get password from JPasswordField

            // Call UserService to authenticate
            authenticatedUser = userService.authenticate(username, password);

            if (authenticatedUser != null) {
                // Authentication successful
                System.out.println("Authentication successful for user: " + username);
                dispose(); // Close the login dialog
            } else {
                // Authentication failed
                System.out.println("Authentication failed for username: " + username);
                JOptionPane.showMessageDialog(
                        this, // Parent component for the message dialog
                        "Incorrect username or password!", // Message text
                        "Login Failed", // Message dialog title
                        JOptionPane.ERROR_MESSAGE // Message type (icon)
                );
                clearFields(); // Clear fields on failure
            }
        });

        // Register button logic
        registerButton.addActionListener(e -> {
            System.out.println("Register button clicked (EDT).");
            // Create and show the Registration dialog
            // Pass this LoginDialog instance as the owner, and the UserService.
            // Assuming RegistrationDialog constructor also needs ExecutorService now
            // If Main passes ExecutorService to LoginDialog, LoginDialog can pass it to RegistrationDialog
            // Assuming Main passes ExecutorService to MenuUI, and MenuUI creates RegistrationDialog indirectly or can pass it.
            // Let's modify LoginDialog to accept ExecutorService as well if RegistrationDialog needs it.
            // For now, let's assume RegistrationDialog can get the ExecutorService if needed (e.g., via a static getter in Main, though injection is better).
            // Assuming Main passes ExecutorService to LoginDialog constructor:
            // RegistrationDialog registrationDialog = new RegistrationDialog(this, userService, executorService); // Requires executorService field in LoginDialog
            // Let's go back to the previous assumption that Main passes ExecutorService to MenuUI, and RegistrationDialog is created somehow with it or doesn't need it directly.
            // If RegistrationDialog gets ExecutorService via Main.getExecutorService():
            // RegistrationDialog registrationDialog = new RegistrationDialog(this, userService); // Call constructor without ExecutorService if it gets it statically

            // Let's correct the design: Main should create ExecutorService and pass it down.
            // LoginDialog constructor should accept ExecutorService. RegistrationDialog constructor should accept ExecutorService.

            // Reverting to the design where Main initializes ExecutorService and passes it to LoginDialog:
            // LoginDialog constructor needs ExecutorService field and parameter.
            // Let's update LoginDialog constructor signature in Main.java too.

            // This code block assumes LoginDialog constructor HAS the ExecutorService field.
            // (This requires adjusting the constructor signature and Main class code)
            // RegistrationDialog registrationDialog = new RegistrationDialog(this, userService, executorService); // Call the constructor with executorService

            // If RegistrationDialog constructor does *not* need ExecutorService (it gets it statically or doesn't do background tasks directly):
            // Assuming RegistrationDialog just calls UserService which uses ExecutorService:
            RegistrationDialog registrationDialog = new RegistrationDialog(this, userService, executorService); // Call the constructor

            registrationDialog.setVisible(true); // Show the registration dialog (This is a blocking call as it's modal)

            // After RegistrationDialog is closed, LoginDialog is still active.
            // Clear login fields after clicking register button.
            clearFields();
        });


        // Cancel button logic
        cancelButton.addActionListener(e -> {
            System.out.println("Cancel button clicked (EDT) in LoginDialog.");
            authenticatedUser = null; // Ensure no user is set on cancel
            dispose(); // Close the login dialog
            System.exit(0); // Exit application completely on cancel
        });

        // Pack the dialog to minimum size based on component preferred sizes
        pack();
        // Center dialog on the screen
        setLocationRelativeTo(null);
    }

    /**
     * Clears the input fields in the login dialog.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow(); // Set focus back to the username field
    }

    /**
     * Shows the login dialog and waits for user authentication.
     * This is a blocking call because the dialog is modal.
     *
     * @return The authenticated User object if login is successful, or null if login fails or is cancelled.
     */
    public User showDialogAndGetResult() {
        setVisible(true); // Show the dialog (This method call blocks the current thread until the dialog is closed)
        return authenticatedUser; // Return the result after the dialog is closed (by dispose())
    }

    // Optional: Add a setter for username field if you want to pre-fill it after registration
    // public void setUsernameFieldText(String username) { this.usernameField.setText(username); }
}