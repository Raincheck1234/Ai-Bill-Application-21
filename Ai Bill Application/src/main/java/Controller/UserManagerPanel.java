package Controller;

import Service.User.UserService;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import java.util.concurrent.ExecutorService;


/**
 * Panel for managing user accounts (Admin only).
 */
public class UserManagerPanel extends JPanel {

    private final UserService userService;
    private final ExecutorService executorService;

    private JTable userTable; // Table to display users
    private DefaultTableModel userTableModel; // Table model for user data
    private JButton addUserButton; // Button to add a new user
    // private JButton editUserButton; // Will add later
    // private JButton deleteUserButton; // Will add later


    /**
     * Constructor to initialize the User Management panel.
     *
     * @param userService The UserService for managing users.
     * @param executorService The ExecutorService for background tasks.
     */
    public UserManagerPanel(UserService userService, ExecutorService executorService) {
        this.userService = userService;
        this.executorService = executorService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Create User Table ---
        String[] columnNames = {"Username", "Role", "Transaction File", "Summary File"};
        userTableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(userTableModel);
        userTable.setFillsViewportHeight(true);
        userTable.setRowHeight(25);

        JScrollPane tableScrollPane = new JScrollPane(userTable);
        add(tableScrollPane, BorderLayout.CENTER);


        // --- Create Control Panel (Top) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        addUserButton = new JButton("Add User");

        controlPanel.add(addUserButton);

        add(controlPanel, BorderLayout.NORTH);


        // --- Add Action Listeners ---
        addUserButton.addActionListener(e -> {
            showAddUserDialog();
        });


        System.out.println("UserManagerPanel initialized.");

        // Refresh data when the panel is initialized
        refreshPanelData();
    }

    /**
     * Refreshes the data displayed in the user management table.
     * Loads the user list from UserService in a background task and updates the table model on EDT.
     */
    public void refreshPanelData() {
        System.out.println("UserManagerPanel refreshPanelData called.");

        // Clear existing table data
        userTableModel.setRowCount(0);
        // Optional: Show a loading message


        // Submit task to ExecutorService to load users
        executorService.submit(() -> {
            System.out.println("UserManagerPanel: Loading users in background...");
            List<User> users = null;
            String errorMessage = null;
            try {
                // Call the public getAllUsers method in UserService
                users = userService.getAllUsers(); // Now calling the public method
                System.out.println("UserManagerPanel: Loaded " + (users != null ? users.size() : 0) + " users.");
            } catch (IOException e) {
                errorMessage = "Failed to load user list:\n" + e.getMessage();
                System.err.println("UserManagerPanel: Error loading users: " + e.getMessage());
                e.printStackTrace();
            }

            final List<User> finalUsers = users;
            final String finalErrorMessage = errorMessage;

            // Update UI on Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                if (finalUsers != null) {
                    for (User user : finalUsers) {
                        Vector<String> rowData = new Vector<>();
                        rowData.add(user.getUsername());
                        rowData.add(user.getRole());
                        rowData.add(user.getTransactionFilePath());
                        rowData.add(user.getSummaryFilePath());
                        userTableModel.addRow(rowData);
                    }
                    System.out.println("UserManagerPanel: User table updated.");
                } else if (finalErrorMessage != null) {
                    JOptionPane.showMessageDialog(this, finalErrorMessage, "Error Loading Users", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    /**
     * Shows the dialog for adding a new user.
     */
    private void showAddUserDialog() {
        // Find the top-level window (JFrame or JDialog) that owns this panel.
        Window window = SwingUtilities.getWindowAncestor(this); // Get the window ancestor

        JDialog ownerDialog = null;
        if (window instanceof JDialog) {
            ownerDialog = (JDialog) window;
        } else if (window instanceof JFrame) {
            // If the owner is a JFrame, you might need a JDialog constructor that accepts a Frame
            // UserDialog constructor currently accepts JDialog owner.
            // Let's assume UserManagerPanel is always within a JDialog or update UserDialog constructor.
            // For now, if it's a JFrame, let's pass null or the JFrame itself if UserDialog accepts Frame.
            // Let's update UserDialog to accept Window.
            System.err.println("UserManagerPanel's owner is a JFrame. UserDialog constructor might need adjustment.");
            // Option: Create a temporary JDialog as owner: new JDialog((Frame) window, true)
        }

        // Create and show the UserDialog for adding a user
        // Pass the found owner window (as JDialog or null if not a JDialog)
        // UserDialog constructor needs to accept Window instead of JDialog
        // Let's update UserDialog constructor signature.
        UserDialog addUserDialog = new UserDialog(window, userService, executorService, null); // Pass the Window as owner
        addUserDialog.setVisible(true);

        // Refresh the user list after dialog closes
        refreshPanelData();
    }

    // TODO: Add showEditUserDialog() and deleteSelectedUser() methods later.


    // Inner classes for ButtonRenderer and ButtonEditor (if they manage user rows)
    // If you add Modify/Delete buttons to UserTable, you might need new Renderers/Editors
    // specific to User management.
}