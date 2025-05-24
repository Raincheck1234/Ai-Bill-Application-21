package Controller;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * Dialog for adding or editing a user account (English).
 */
public class UserDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField roleField;

    private final UserService userService;
    private final ExecutorService executorService;

    private final User userToEdit; // The User object being edited (null for adding)

    /**
     * Constructor for the UserDialog.
     *
     * @param owner The parent Window (Frame or Dialog) that owns this dialog.
     * @param userService The UserService for managing users.
     * @param executorService The ExecutorService for background tasks.
     * @param userToEdit The User object to edit, or null for adding a new user.
     */
    public UserDialog(Window owner, UserService userService, ExecutorService executorService, User userToEdit) { // Accept Window as owner
        // Call super constructor with owner, title, and modal property
        // Need to cast owner to Frame or Dialog if super() requires it.
        // JDialog(Window owner, String title, boolean modal) is a valid constructor.
        super((Frame) owner, userToEdit == null ? "Add User" : "Edit User", true); // Title changes based on action
        this.userService = userService;
        this.executorService = executorService;
        this.userToEdit = userToEdit;

        // Set layout for the main dialog panel
        JPanel mainDialogPanel = new JPanel(new BorderLayout(10, 10));
        mainDialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input fields panel using GridLayout
        JPanel inputFieldsPanel = new JPanel(new GridLayout(4, 2, 10, 10)); // 4 rows for Username, Password, Confirm Pwd, Role

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        roleField = new JTextField("user");

        inputFieldsPanel.add(new JLabel("Username:")); inputFieldsPanel.add(usernameField);
        inputFieldsPanel.add(new JLabel("Password:")); inputFieldsPanel.add(passwordField);
        inputFieldsPanel.add(new JLabel("Confirm Password:")); inputFieldsPanel.add(confirmPasswordField);
        inputFieldsPanel.add(new JLabel("Role:")); inputFieldsPanel.add(roleField);

        // Populate fields if editing
        if (userToEdit != null) {
            populateFieldsForEdit(); // Call helper method
        } else {
            clearFields(); // Clear fields for adding a new user
        }


        mainDialogPanel.add(inputFieldsPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton confirmButton = new JButton(userToEdit == null ? "Add" : "Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        mainDialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainDialogPanel); // Add the main panel to the dialog


        // --- Define the modal waiting dialog ---
        JDialog waitingDialog = new JDialog(this, "Please wait", true); // Owned by this dialog
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel(userToEdit == null ? "Adding user..." : "Saving user..."));
        waitingDialog.setSize(200, 100);
        waitingDialog.setResizable(false);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);


        // --- Add Action Listeners ---

        cancelButton.addActionListener(e -> {
            dispose(); // Close the dialog
        });

        confirmButton.addActionListener(e -> {
            System.out.println("Confirm button clicked in UserDialog (EDT).");

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            String role = roleField.getText().trim();


            // --- Input Validation ---
            if (username.isEmpty() || role.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and Role cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (userToEdit == null) { // Validation specific to Adding User
                if (password.isEmpty() || confirmPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Password and Confirm Password cannot be empty for a new user.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(this, "Password and Confirm Password do not match.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    passwordField.setText("");
                    confirmPasswordField.setText("");
                    return;
                }
                if (!role.equalsIgnoreCase("user") && !role.equalsIgnoreCase("admin")) {
                    JOptionPane.showMessageDialog(this, "Role must be 'user' or 'admin'.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

            } else { // Validation specific to Editing User
                // Username field is disabled, so no need to validate its content.
                boolean passwordFieldsFilled = !password.isEmpty() || !confirmPassword.isEmpty();
                if (passwordFieldsFilled) {
                    if (!password.equals(confirmPassword)) {
                        JOptionPane.showMessageDialog(this, "If changing password, Password and Confirm Password must match.", "Input Error", JOptionPane.WARNING_MESSAGE);
                        passwordField.setText("");
                        confirmPasswordField.setText("");
                        return;
                    }
                }
                // Basic role validation for edit
                if (!role.equalsIgnoreCase("user") && !role.equalsIgnoreCase("admin")) {
                    JOptionPane.showMessageDialog(this, "Role must be 'user' or 'admin'.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }


            // --- Disable buttons and show waiting dialog ---
            confirmButton.setEnabled(false);
            cancelButton.setEnabled(false);
            waitingDialog.setLocationRelativeTo(this);
            waitingDialog.setVisible(true); // THIS CALL BLOCKS THE EDT


            // --- Submit the task to the ExecutorService ---
            // Task to perform depends on whether adding or editing
            Runnable userTask;
            if (userToEdit == null) { // Add User Task
                userTask = () -> {
                    System.out.println("User Task: Calling userService.registerUser for " + username);
                    String message;
                    boolean success = false;
                    try {
                        success = userService.registerUser(username, password, role);
                        if (success) message = "User '" + username + "' registered successfully!";
                        else message = "An unexpected registration error occurred."; // Should be caught by exception or return false with specific reason from service

                    } catch (IllegalArgumentException ex) {
                        message = "Registration failed:\n" + ex.getMessage(); // Includes 'Username already exists'
                        System.err.println("Error during user task (Registration - IllegalArgumentException): " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        message = "Registration failed due to an error!\n" + ex.getMessage();
                        System.err.println("Error during user task (Registration - Exception): " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    // Schedule UI update
                    String finalMessage = message;
                    boolean finalSuccess = success;
                    SwingUtilities.invokeLater(() -> {
                        waitingDialog.dispose();
                        JOptionPane.showMessageDialog(this, finalMessage, "Registration Result", JOptionPane.INFORMATION_MESSAGE);
                        if (finalSuccess) dispose(); // Close on success
                        else { confirmButton.setEnabled(true); cancelButton.setEnabled(true); clearFields(); } // Re-enable on failure
                    });
                };
            } else { // Edit User Task
                // Create updated User object with only changed fields (or all if simpler)
                User updatedUserInfo = new User();
                updatedUserInfo.setUsername(userToEdit.getUsername()); // Username is the key, must be the original
                updatedUserInfo.setRole(role); // Role might have changed

                // Only set password if user entered something
                boolean passwordFieldsFilled = !password.isEmpty() || !confirmPassword.isEmpty();
                if (passwordFieldsFilled) {
                    updatedUserInfo.setPassword(password);
                } else {
                    updatedUserInfo.setPassword(null); // Pass null to indicate no password change
                }

                userTask = () -> {
                    System.out.println("User Task: Calling userService.updateUser for " + userToEdit.getUsername());
                    String message;
                    boolean success = false;
                    try {
                        success = userService.updateUser(updatedUserInfo);
                        if (success) message = "User '" + userToEdit.getUsername() + "' updated successfully!";
                        else message = "Update failed. User not found or data invalid."; // Should be caught by exception

                    } catch (IllegalArgumentException ex) {
                        message = "Update failed:\n" + ex.getMessage();
                        System.err.println("Error during user task (Update - IllegalArgumentException): " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        message = "Update failed due to an error!\n" + ex.getMessage();
                        System.err.println("Error during user task (Update - IOException): " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        message = "Update failed due to an unexpected error!\n" + ex.getMessage();
                        System.err.println("Error during user task (Update - Exception): " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    // Schedule UI update
                    String finalMessage = message;
                    boolean finalSuccess = success;
                    SwingUtilities.invokeLater(() -> {
                        waitingDialog.dispose();
                        JOptionPane.showMessageDialog(this, finalMessage, "Update Result", JOptionPane.INFORMATION_MESSAGE);
                        if (finalSuccess) dispose(); // Close on success
                        else { confirmButton.setEnabled(true); cancelButton.setEnabled(true); /* Optional: clear password fields */ } // Re-enable on failure
                    });
                };
            }

            executorService.submit(userTask); // Submit the task to the pool
            System.out.println("User task submitted to ExecutorService.");

            // The waiting dialog blocks EDT here until task finishes and disposes it.
            System.out.println("Waiting dialog is now hidden (EDT unblocked)."); // This prints after task finishes and disposes the dialog

        });


        // Pack dialog to minimum size and center it
        pack();
        setLocationRelativeTo(owner); // Center relative to the owner
    }

    /**
     * Populates the dialog fields with user data when editing.
     * Called internally by the constructor when userToEdit is not null.
     */
    private void populateFieldsForEdit() {
        if (userToEdit != null) {
            usernameField.setText(userToEdit.getUsername());
            usernameField.setEditable(false); // Username cannot be changed when editing
            // passwordField and confirmPasswordField are left empty by default for security
            roleField.setText(userToEdit.getRole());
        }
    }

    /**
     * Clears the input fields in the dialog.
     * Called on dialog initialization (if adding) or on validation failure.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        roleField.setText("user"); // Reset to default role
        usernameField.requestFocusInWindow();
    }
}