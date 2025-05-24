package Interceptor.Login;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Future; // Import Future if needed


/**
 * Dialog for new user registration (English).
 */
public class RegistrationDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    private final UserService userService; // Injected UserService
    private final ExecutorService executorService; // NEW: ExecutorService field


    /**
     * Constructor for the RegistrationDialog.
     *
     * @param owner           The parent Frame or Dialog (e.g., LoginDialog instance).
     * @param userService     The UserService instance for registration logic.
     * @param executorService Executor service for background tasks.
     */
    public RegistrationDialog(JDialog owner, UserService userService, ExecutorService executorService) { // Accept ExecutorService
        super(owner, "User Registration", true);
        this.userService = userService;
        this.executorService = executorService; // Assign ExecutorService

        setLayout(new GridLayout(4, 2, 10, 10));
        setSize(350, 200);
        setResizable(false);

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        JButton registerButton = new JButton("Register");
        JButton cancelButton = new JButton("Cancel");

        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(new JLabel("Confirm Password:"));
        add(confirmPasswordField);
        add(registerButton);
        add(cancelButton);


        // --- Define the modal waiting dialog for registration process ---
        JDialog waitingDialog = new JDialog(this, "Please wait", true);
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel("Registering user..."));
        waitingDialog.setSize(200, 100);
        waitingDialog.setResizable(false);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);


        // --- Add Action Listeners ---

        // Cancel button logic
        cancelButton.addActionListener(e -> {
            System.out.println("Cancel button clicked (EDT) in RegistrationDialog.");
            dispose();
        });

        // Register button logic - Uses ExecutorService
        registerButton.addActionListener(e -> {
            System.out.println("Register button clicked (EDT).");

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            String role = "user";


            // --- Basic Input Validation (UI level) ---
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Password and Confirm Password do not match.", "Input Error", JOptionPane.WARNING_MESSAGE);
                clearFields();
                return;
            }
            // Optional: More password strength validation


            // --- Show waiting dialog and submit task to ExecutorService ---
            // 1. Disable button immediately on EDT
            registerButton.setEnabled(false);
            cancelButton.setEnabled(false);

            // 2. Show the modal waiting dialog LAST in the EDT block
            waitingDialog.setLocationRelativeTo(this);
            waitingDialog.setVisible(true); // THIS CALL BLOCKS THE EDT


            // 3. Submit the registration task to the ExecutorService
            // This happens *after* setVisible(true) returns, because EDT is blocked.
            executorService.submit(() -> {
                System.out.println("Registration task submitted to ExecutorService...");
                String message;
                boolean success = false;
                try {
                    // Thread.sleep(3000); // Simulate delay

                    System.out.println("Registration Task: Calling userService.registerUser for " + username);
                    success = userService.registerUser(username, password, role);
                    System.out.println("Registration Task: userService.registerUser returned " + success);

                    if (success) {
                        message = "Registration successful!";
                    } else {
                        // Assuming UserService throws IllegalArgumentException for username exists
                        // Message will be set in the catch block below
                        // If registerUser returns false without exception, adjust logic here.
                        // Based on Step 9.2, it throws Exception or IllegalArgumentException on failure.
                        // So success=false implies an exception was thrown.
                        message = "An unexpected error occurred."; // Fallback if success=false but no exception caught
                    }

                } catch (IllegalArgumentException ex) {
                    message = "Registration failed due to invalid data or existing username:\n" + ex.getMessage();
                    System.err.println("Error during registration task (IllegalArgumentException):");
                    ex.printStackTrace();
                    success = false;
                } catch (Exception ex) {
                    message = "Registration failed due to an error!\n" + ex.getMessage();
                    System.err.println("Error during registration task (Exception):");
                    ex.printStackTrace();
                    success = false;
                }

                // 4. Schedule UI update on Event Dispatch Thread (EDT)
                String finalMessage = message;
                boolean finalSuccess = success;
                System.out.println("Registration Task: Scheduling UI update on EDT.");

                SwingUtilities.invokeLater(() -> {
                    System.out.println("EDT: Running UI update after registration task.");
                    waitingDialog.dispose();
                    System.out.println("EDT: waitingDialog disposed.");

                    JOptionPane.showMessageDialog(this, finalMessage,
                            finalSuccess ? "Registration Success" : "Registration Failed",
                            finalSuccess ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                    System.out.println("EDT: Registration result dialog shown.");

                    if (finalSuccess) {
                        dispose();
                        System.out.println("EDT: Registration dialog disposed (success).");
                    } else {
                        registerButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        clearFields();
                        System.out.println("EDT: Buttons re-enabled, fields cleared (failure).");
                    }
                    System.out.println("EDT: UI update complete.");
                });
                System.out.println("Registration Task: Finished run method.");
            });
            System.out.println("Showing waiting dialog returned (EDT unblocked)."); // This prints after the task finishes and disposes the dialog
        });

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Clears the input fields in the registration dialog.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        usernameField.requestFocusInWindow();
    }
}