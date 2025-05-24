package Interceptor.Login;

import Constants.StandardCategories; // Keep import if needed for validation
import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List; // Keep if used elsewhere

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Future; // Import Future if needed


/**
 * Dialog for new user registration (English).
 */
public class RegistrationDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    // If adding role selection: private JComboBox<String> roleComboBox;

    private final UserService userService; // Injected UserService
    private final ExecutorService executorService; // NEW: ExecutorService field

    /**
     * Constructor for the RegistrationDialog.
     * @param owner The parent Frame or Dialog (e.g., LoginDialog instance).
     * @param userService The UserService instance for registration logic.
     * @param executorService Executor service for background tasks.
     */
    public RegistrationDialog(JDialog owner, UserService userService, ExecutorService executorService) { // Accept ExecutorService
        // Call super constructor with owner, title, and modal property
        super(owner, "User Registration", true); // Title in English
        this.userService = userService; // Assign injected service
        this.executorService = executorService; // Assign ExecutorService

        // Set the layout manager: 4 rows, 2 columns, with horizontal and vertical gaps
        setLayout(new GridLayout(4, 2, 10, 10)); // 4 rows, 2 columns, with gaps
        setSize(350, 200); // Adjusted size for 4x2 layout
        setResizable(false);

        // Create input components
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        // if adding role selection: roleComboBox = new JComboBox<>(new String[]{"user"}); // Assuming default role is 'user'

        // Create buttons
        JButton registerButton = new JButton("Register"); // Button text in English
        JButton cancelButton = new JButton("Cancel");     // Button text in English

        // --- Add components to the dialog following the GridLayout (Row by Row, Column by Column) ---
        // Row 0
        add(new JLabel("Username:"));           // Row 0, Col 0 (Label)
        add(usernameField);                     // Row 0, Col 1 (Field)

        // Row 1
        add(new JLabel("Password:"));           // Row 1, Col 0 (Label)
        add(passwordField);                     // Row 1, Col 1 (Field)

        // Row 2
        add(new JLabel("Confirm Password:"));   // Row 2, Col 0 (Label)
        add(confirmPasswordField);              // Row 2, Col 1 (Field)

        // Row 3 (Buttons)
        // Place buttons directly in GridLayout cells
        add(registerButton);                    // Row 3, Col 0 (Register Button)
        add(cancelButton);                      // Row 3, Col 1 (Cancel Button)

        // If adding role selection, insert here (e.g., before Confirm Password, adjusting GridLayout rows)
        // add(new JLabel("Role:")); add(roleComboBox);


        // --- Define the modal waiting dialog for registration process ---
        JDialog waitingDialog = new JDialog(this, "Please wait", true); // Modal dialog owned by RegistrationDialog itself
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel("Registering user..."));
        waitingDialog.setSize(200, 100);
        waitingDialog.setResizable(false);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Prevent closing with X button


        // --- Add Action Listeners ---

        // Cancel button logic
        cancelButton.addActionListener(e -> {
            System.out.println("Cancel button clicked (EDT) in RegistrationDialog.");
            dispose(); // Close the registration dialog
        });

        // Register button logic - Uses ExecutorService
        registerButton.addActionListener(e -> {
            System.out.println("Register button clicked (EDT).");

            // Get input values
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            // String role = (String) roleComboBox.getSelectedItem(); // if using role selection
            String role = "user"; // Assume default role is 'user' for registration


            // --- Basic Input Validation (UI level) ---
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return; // Stop if fields are empty
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Password and Confirm Password do not match.", "Input Error", JOptionPane.WARNING_MESSAGE);
                clearFields(); // Clear password fields
                return; // Stop if passwords don't match
            }
            // Optional: More password strength validation


            // --- Show waiting dialog and submit task to ExecutorService ---
            // 1. Disable button immediately on EDT
            registerButton.setEnabled(false);
            cancelButton.setEnabled(false); // Also disable cancel button

            // 2. Show the modal waiting dialog LAST in the EDT block
            // This call will block the EDT until waitingDialog.dispose() is called from the task.
            waitingDialog.setLocationRelativeTo(this); // Center waiting dialog relative to registration dialog
            waitingDialog.setVisible(true); // THIS CALL BLOCKS THE EDT


            // 3. Submit the registration task to the ExecutorService
            // This happens *after* setVisible(true) returns, because EDT is blocked.
            // This is the pattern for modal dialogs triggered from EDT.
            executorService.submit(() -> { // Use submit
                System.out.println("Registration task submitted to ExecutorService...");
                String message;
                boolean success = false;
                try {
                    // Thread.sleep(3000); // Simulate delay for testing thread behavior

                    // Call the actual service method (Implemented in Step 9.2)
                    System.out.println("Registration Task: Calling userService.registerUser for " + username);
                    success = userService.registerUser(username, password, role); // Pass collected data
                    System.out.println("Registration Task: userService.registerUser returned " + success);

                    if (success) {
                        message = "Registration successful!";
                    } else {
                        // UserService.registerUser should throw an exception or return false with reason (e.g., username exists)
                        // Assuming it throws IllegalArgumentException for username exists:
                        // This catch block below handles IllegalArgumentException
                        // If it just returns false without exception, the message needs to be set here.
                        // Let's rely on the Exception/IllegalArgumentException catches below for failure messages.
                        // If success is false here, but no exception, it means a handled business logic error occurred (like username exists).
                        if (!success) {
                            message = "Registration failed. Username might already exist.";
                        } else {
                            message = "Unexpected registration success=false without exception."; // Should not happen if success is boolean
                        }
                    }

                } catch (IllegalArgumentException ex) { // Catch validation errors from service (e.g., username exists, invalid data)
                    message = "Registration failed due to invalid data or existing username:\n" + ex.getMessage();
                    System.err.println("Error during registration task (IllegalArgumentException):");
                    ex.printStackTrace();
                    success = false; // Ensure success is false on error
                }
                catch (Exception ex) { // Catch other exceptions from service (e.g., IOException from file operations)
                    message = "Registration failed due to an error!\n" + ex.getMessage();
                    System.err.println("Error during registration task (Exception):");
                    ex.printStackTrace();
                    success = false; // Ensure success is false on error
                }

                // 4. Schedule UI update on Event Dispatch Thread (EDT)
                String finalMessage = message;
                boolean finalSuccess = success; // Use the actual success flag
                System.out.println("Registration Task: Scheduling UI update on EDT.");

                SwingUtilities.invokeLater(() -> {
                    System.out.println("EDT: Running UI update after registration task.");
                    // --- Hide waiting dialog ---
                    waitingDialog.dispose(); // Dispose the waiting dialog
                    System.out.println("EDT: waitingDialog disposed.");

                    // --- Show result message to user ---
                    JOptionPane.showMessageDialog(this, finalMessage,
                            finalSuccess ? "Registration Success" : "Registration Failed",
                            finalSuccess ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                    System.out.println("EDT: Registration result dialog shown.");

                    // --- Handle main dialog closing and button re-enabling ---
                    if (finalSuccess) {
                        dispose(); // Close registration dialog on success
                        System.out.println("EDT: Registration dialog disposed (success).");
                    } else {
                        // Re-enable buttons if registration failed but dialog is still open
                        registerButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        clearFields(); // Clear fields on failure
                        System.out.println("EDT: Buttons re-enabled, fields cleared (failure).");
                    }
                    System.out.println("EDT: UI update complete.");
                });
                System.out.println("Registration Task: Finished run method.");
            });

            // The line waitingDialog.setVisible(true) above is what blocks.
            // The code below runs only after the thread finishes and disposes the dialog.
            System.out.println("Showing waiting dialog returned (EDT unblocked).");

        });


        // Pack dialog to minimum size and center it
        pack();
        setLocationRelativeTo(owner); // Center relative to the owner (LoginDialog)
    }

    /**
     * Clears the input fields in the registration dialog.
     */
    private void clearFields() {
        usernameField.setText(""); // Also clear username field on failure
        passwordField.setText("");
        confirmPasswordField.setText("");
        usernameField.requestFocusInWindow(); // Focus back to username
    }
}