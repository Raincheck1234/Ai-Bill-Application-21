package Controller;

import Constants.StandardCategories;
import Service.AIservice.AITransactionService;
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.SummaryStatisticService;
import Service.TransactionService;
import model.SummaryStatistic;
import model.Transaction;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Comparator;
import java.util.stream.Collectors;

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Future; // Import Future if needed for task management

public class MenuUI extends JPanel {

    private final User currentUser;
    private final TransactionService transactionService;
    private final SummaryStatisticService summaryStatisticService;
    private final AITransactionService aiTransactionService;
    private final CollegeStudentNeeds collegeStudentNeeds;
    private final ExecutorService executorService; // ExecutorService field

    private DefaultTableModel tableModel;

    // Fields for search input components
    private JTextField searchTransactionTimeField;
    private JTextField searchTransactionTypeField;
    private JTextField searchCounterpartyField;
    private JTextField searchCommodityField;
    private JComboBox<String> searchInOutComboBox;
    private JTextField searchPaymentMethodField;
    private JButton searchButton;

    private JTable table;

    private JPanel rightPanel;
    private CardLayout cardLayout;

    // UI components for AI panel
    private JTextArea aiResultArea;
    private JTextField aiStartTimeField;
    private JTextField aiEndTimeField;
    private JButton aiAnalyzeButton;
    private JButton aiBudgetButton;
    private JButton aiTipsButton;
    private JButton aiPersonalSummaryButton;
    private JButton aiSavingsGoalsButton;
    private JButton aiPersonalSavingTipsButton;
    private JButton runBatchAiButton;

    // UI components for Admin Stats panel
    private JTextArea adminStatsArea;
    private JButton generateStatsButton;
    private JButton refreshDisplayButton;

    // Panel for Visualization
    private VisualizationPanel visualizationPanel;


    /**
     * Constructor to initialize the main UI panel and its components.
     *
     * @param authenticatedUser The currently logged-in user.
     * @param transactionService User-specific transaction service.
     * @param summaryStatisticService Summary statistics service (for admin).
     * @param aiTransactionService AI transaction service.
     * @param collegeStudentNeeds College student specific AI service.
     * @param executorService Executor service for background tasks.
     */
    public MenuUI(User authenticatedUser, TransactionService transactionService,
                  SummaryStatisticService summaryStatisticService,
                  AITransactionService aiTransactionService,
                  CollegeStudentNeeds collegeStudentNeeds,
                  ExecutorService executorService) { // Accept ExecutorService

        this.currentUser = authenticatedUser;
        this.transactionService = transactionService;
        this.summaryStatisticService = summaryStatisticService;
        this.aiTransactionService = aiTransactionService;
        this.collegeStudentNeeds = collegeStudentNeeds;
        this.executorService = executorService; // Assign ExecutorService

        // Initialize table model
        String[] columnNames = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks", "Modify", "Delete"};
        this.tableModel = new DefaultTableModel(columnNames, 0);
        this.table = new JTable(this.tableModel);

        // Set the layout manager for this JPanel (MenuUI)
        setLayout(new BorderLayout());

        // Add the left navigation panel
        add(createLeftPanel(), BorderLayout.WEST);

        // Add the right content panel (uses CardLayout)
        setupRightPanel();
        add(rightPanel, BorderLayout.CENTER);

        // Initial data load happens after the main panel is created and added to a frame.
        // We'll call loadCSVDataForCurrentUser("") in createMainPanel().

        System.out.println("MenuUI initialized for user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
    }

    /**
     * Creates and returns the main JPanel for the MenuUI.
     * This method is typically called once from Main.java after the MenuUI instance is created.
     *
     * @return The main JPanel (which is the MenuUI instance itself).
     */
    public JPanel createMainPanel() {
        // The MenuUI JPanel itself is the main panel.
        // Load initial data here now that the UI components are set up.
        loadCSVDataForCurrentUser(""); // Load all data initially

        return this; // Return this MenuUI instance
    }


    /**
     * Loads CSV data for the current user and populates the table model.
     *
     * @param initialInOutFilter Optional filter ("Income", "Expense", or empty for all).
     */
    public void loadCSVDataForCurrentUser(String initialInOutFilter) {
        this.tableModel.setRowCount(0); // Clear the table model

        try {
            List<Transaction> transactions = transactionService.getAllTransactions();
            System.out.println("Loaded total " + transactions.size() + " transactions from service for user " + currentUser.getUsername());

            List<Transaction> filteredTransactions = new java.util.ArrayList<>();
            if (initialInOutFilter == null || initialInOutFilter.trim().isEmpty()) {
                filteredTransactions.addAll(transactions);
            } else {
                String filter = initialInOutFilter.trim();
                // Filter based on In/Out, handling potential variations ("Income"/"收", "Expense"/"支", "In"/"Out")
                filteredTransactions = transactions.stream()
                        .filter(t -> t.getInOut() != null && (t.getInOut().equalsIgnoreCase(filter) ||
                                (filter.equalsIgnoreCase("Income") && (t.getInOut().equalsIgnoreCase("收") || t.getInOut().equalsIgnoreCase("In"))) ||
                                (filter.equalsIgnoreCase("Expense") && (t.getInOut().equalsIgnoreCase("支") || t.getInOut().equalsIgnoreCase("Out"))) ))
                        .collect(Collectors.toList());
            }

            for (Transaction transaction : filteredTransactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                this.tableModel.addRow(row);
            }
            System.out.println("Displayed " + filteredTransactions.size() + " transactions in the table.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load user transaction data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates the left navigation panel with buttons.
     * @return The left panel.
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton menuButton = new JButton("Transaction List");
        JButton aiButton = new JButton("AI Analysis");
        JButton adminButton = new JButton("Admin Stats");
        JButton visualizationButton = new JButton("Visualization");

        Dimension buttonSize = new Dimension(150, 40);
        menuButton.setMaximumSize(buttonSize);
        aiButton.setMaximumSize(buttonSize);
        adminButton.setMaximumSize(buttonSize);
        visualizationButton.setMaximumSize(buttonSize);


        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        aiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        visualizationButton.setAlignmentX(Component.CENTER_ALIGNMENT);


        leftPanel.add(menuButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(aiButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            leftPanel.add(adminButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            System.out.println("Admin user logged in, showing Admin button.");
        } else {
            System.out.println("Regular user logged in, hiding Admin button.");
        }

        leftPanel.add(visualizationButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        // Add action listeners for navigation buttons
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table");
            // Decide if you want to reload all data or just income when returning to table
            loadCSVDataForCurrentUser("Income"); // Example: show only Income by default on return
        });

        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "AI");
            // Optional: Clear AI results area or show default message when switching
            // aiResultArea.setText("Welcome to the AI Personal Finance Analysis feature.\n\n...");
        });

        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            adminButton.addActionListener(e -> {
                cardLayout.show(rightPanel, "AdminStats");
                displaySummaryStatistics(); // Refresh stats display when switching
            });
        }

        visualizationButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Visualization");
            if (visualizationPanel != null) {
                visualizationPanel.refreshPanelData(); // Call refresh method on VisualizationPanel
            }
        });

        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    /**
     * Sets up the right panel with different views using CardLayout.
     */
    private void setupRightPanel() {
        this.cardLayout = new CardLayout();
        this.rightPanel = new JPanel(this.cardLayout);

        JPanel tablePanel = createTablePanel(); // Table view
        JPanel aiPanel = createAIPanel(); // AI view
        JPanel adminStatsPanel = createAdminStatsPanel(); // Admin stats view
        this.visualizationPanel = new VisualizationPanel(this.transactionService); // Create VisualizationPanel, inject TransactionService


        rightPanel.add(tablePanel, "Table");
        rightPanel.add(aiPanel, "AI");
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            rightPanel.add(adminStatsPanel, "AdminStats");
        }
        rightPanel.add(visualizationPanel, "Visualization");


        cardLayout.show(rightPanel, "Table"); // Set the initially visible card
    }

    /**
     * Creates the panel for displaying and managing transaction data in a table.
     * @return The table panel.
     */
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        JPanel inputPanel = createInputPanel();
        tablePanel.add(inputPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(this.table);
        tableScrollPane.setPreferredSize(new Dimension(1000, 250));
        this.table.setFillsViewportHeight(true);
        this.table.setRowHeight(30);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Set cell renderers and editors for action buttons
        this.table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(this));

        this.table.getColumnModel().getColumn(12).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(12).setCellEditor(new ButtonEditor(this));

        return tablePanel;
    }

    /**
     * Creates the input panel for searching and adding transactions.
     * @return The input panel.
     */
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // Create input fields and capture references
        searchTransactionTimeField = new JTextField(10);
        searchTransactionTypeField = new JTextField(10);
        searchCounterpartyField = new JTextField(10);
        searchCommodityField = new JTextField(10);
        searchInOutComboBox = new JComboBox<>(new String[]{"", "Income", "Expense"});
        searchPaymentMethodField = new JTextField(10);

        inputPanel.add(new JLabel("Transaction Time:")); inputPanel.add(searchTransactionTimeField);
        inputPanel.add(new JLabel("Transaction Type:")); inputPanel.add(searchTransactionTypeField);
        inputPanel.add(new JLabel("Counterparty:")); inputPanel.add(searchCounterpartyField);
        inputPanel.add(new JLabel("Commodity:")); inputPanel.add(searchCommodityField);
        inputPanel.add(new JLabel("In/Out:")); inputPanel.add(searchInOutComboBox);
        inputPanel.add(new JLabel("Payment Method:")); inputPanel.add(searchPaymentMethodField);

        JButton searchButton = new JButton("Search");
        JButton addButton = new JButton("Add");
        JButton importButton = new JButton("Import CSV");

        inputPanel.add(searchButton);
        inputPanel.add(addButton);
        inputPanel.add(importButton);

        searchButton.addActionListener(e -> triggerCurrentSearch());
        addButton.addActionListener(e -> showAddTransactionDialog());

        importButton.addActionListener(e -> {
            showImportDialog();
        });

        return inputPanel;
    }

    /**
     * Shows the dialog for importing transactions from a CSV file.
     */
    private void showImportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV file to import");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToImport = fileChooser.getSelectedFile();
            String filePath = fileToImport.getAbsolutePath();
            System.out.println("User selected file for import: " + filePath);

            // Submit import task to the ExecutorService
            executorService.submit(() -> {
                System.out.println("Import task submitted to ExecutorService.");
                String message;
                try {
                    // Call the service method to handle the import logic
                    int importedCount = transactionService.importTransactionsFromCsv(currentUser.getTransactionFilePath(), filePath);

                    message = "Successfully imported " + importedCount + " transaction records.";
                    System.out.println("Import task finished: " + message);

                    String finalMessage = message;
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        loadCSVDataForCurrentUser(""); // Reload all data after adding/importing
                        clearSearchFields(); // Clear search fields after reload
                        JOptionPane.showMessageDialog(this, finalMessage, "Import Successful", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception ex) {
                    message = "Import failed!\n" + ex.getMessage();
                    System.err.println("Import task failed: " + ex.getMessage());
                    ex.printStackTrace();
                    String finalMessage1 = message;
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        JOptionPane.showMessageDialog(this, finalMessage1, "Import Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });

        } else {
            System.out.println("User cancelled file selection.");
        }
    }

    /**
     * Shows the dialog for adding a new transaction.
     */
    private void showAddTransactionDialog() {
        JDialog addDialog = new JDialog();
        addDialog.setTitle("Add Transaction");
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Default constraints
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField transactionTimeField = new JTextField(15);
        JTextField transactionTypeField = new JTextField(15);
        JButton aiSuggestButton = new JButton("AI Category Suggestion");

        JTextField counterpartyField = new JTextField(15);
        JTextField commodityField = new JTextField(15);
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"Income", "Expense"});
        JTextField paymentAmountField = new JTextField(15);
        JTextField paymentMethodField = new JTextField(15);
        JTextField currentStatusField = new JTextField(15);
        JTextField orderNumberField = new JTextField(15); // Order Number field (should be editable for add)
        JTextField merchantNumberField = new JTextField(15);
        JTextField remarksField = new JTextField(15);

        // Add components using GridBagLayout - Corrected layout logic
        String[] fieldNames = {
                "Transaction Time", "Transaction Type", "Counterparty", "Commodity",
                "In/Out", "Amount(CNY)", "Payment Method", "Current Status",
                "Order Number", "Merchant Number", "Remarks"
        };
        // Array of JTextFields for easier access (excluding JComboBox)
        JTextField[] textFields = {
                transactionTimeField, transactionTypeField, counterpartyField,
                commodityField, paymentAmountField, paymentMethodField, currentStatusField,
                orderNumberField, merchantNumberField, remarksField
        };
        int textFieldIndex = 0;

        for (int i = 0; i < fieldNames.length; i++) {
            // Constraints for Label (Column 0, Row i)
            gbc.gridx = 0; gbc.gridy = i; gbc.gridwidth = 1; gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
            dialogPanel.add(new JLabel(fieldNames[i] + ":"), gbc);

            // Reset constraints for the input component on the same row
            gbc.gridx = 1; gbc.gridy = i; gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.HORIZONTAL;

            if (fieldNames[i].equals("Transaction Type")) { // Row for Transaction Type and AI button
                gbc.gridwidth = 1; // Field takes 1 column
                dialogPanel.add(textFields[textFieldIndex++], gbc); // Add Transaction Type field

                // Constraints for AI Suggest Button (Column 2, Same Row i)
                gbc.gridx = 2; gbc.gridy = i; gbc.gridwidth = 1; gbc.weightx = 0.0;
                gbc.fill = GridBagConstraints.NONE; // Button doesn't fill space
                dialogPanel.add(aiSuggestButton, gbc);

            } else if (fieldNames[i].equals("In/Out")) { // Row for In/Out ComboBox
                gbc.gridwidth = 2; // ComboBox spans 2 columns
                dialogPanel.add(inOutComboBox, gbc);

            } else { // All other TextFields
                gbc.gridwidth = 2; // These fields span 2 columns (1 and 2)
                dialogPanel.add(textFields[textFieldIndex++], gbc); // Add the text field
            }
        }

        // Order Number field should be editable for adding
        orderNumberField.setEditable(true);


        // --- Define the modal waiting dialog for AI suggestion ---
        JDialog waitingDialog = new JDialog(addDialog, "Please wait", true);
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel("Getting AI category suggestion..."));
        waitingDialog.setSize(250, 100);
        waitingDialog.setResizable(false);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Prevent closing with X


        // Add Confirm and Cancel buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("Confirm");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // Constraints for Button Panel (placed below the last field row)
        gbc.gridx = 0; gbc.gridy = fieldNames.length; // Row 11 (after 0-10)
        gbc.gridwidth = 3; // Span all 3 columns
        gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(15, 5, 5, 5);
        gbc.weightx = 0.0; gbc.weighty = 1.0; // Give this row vertical weight to push fields up
        dialogPanel.add(buttonPanel, gbc);


        addDialog.add(dialogPanel, BorderLayout.CENTER);


        // Add AI Suggest button action listener - Uses ExecutorService
        aiSuggestButton.addActionListener(e -> {
            System.out.println("AI Suggest button clicked (EDT).");

            // 1. Disable button immediately on EDT
            aiSuggestButton.setEnabled(false);
            // confirmButton.setEnabled(false); // Optional: also disable confirm

            // 2. Build temporary transaction object from current fields
            // Get values from textFields array + ComboBox
            Transaction tempTransaction = new Transaction(
                    textFields[0].getText().trim(), // Transaction Time
                    textFields[1].getText().trim(), // Transaction Type
                    textFields[2].getText().trim(), // Counterparty
                    textFields[3].getText().trim(), // Commodity
                    (String) inOutComboBox.getSelectedItem(), // In/Out (ComboBox)
                    safeParseDouble(textFields[4].getText().trim()), // Amount
                    textFields[5].getText().trim(), // Payment Method
                    textFields[6].getText().trim(), // Current Status
                    textFields[7].getText().trim(), // Order Number
                    textFields[8].getText().trim(), // Merchant Number
                    textFields[9].getText().trim()  // Remarks
            );

            // 3. Submit the AI task to the ExecutorService
            executorService.submit(() -> {
                System.out.println("AI Suggest task submitted to ExecutorService...");
                String aiSuggestion = null;
                try {
                    // Thread.sleep(3000); // Simulate delay
                    aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);
                    System.out.println("AI Suggest task finished. Result: " + aiSuggestion);
                } catch (Exception ex) {
                    System.err.println("Error in AI Suggest task: " + ex.getMessage());
                    ex.printStackTrace();
                    aiSuggestion = "Error: " + ex.getMessage(); // Capture error
                }

                // 4. Schedule UI update on Event Dispatch Thread (EDT)
                String finalSuggestion = aiSuggestion;
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Updating UI on EDT after AI Suggest task.");
                    // --- Hide waiting dialog ---
                    waitingDialog.setVisible(false); // This hides the modal dialog

                    // --- Display AI suggestion ---
                    if (finalSuggestion != null && !finalSuggestion.isEmpty() && !finalSuggestion.startsWith("Error:")) {
                        // Safety Check against standard categories
                        if (StandardCategories.ALL_KNOWN_TYPES.contains(finalSuggestion.trim())) {
                            textFields[1].setText(finalSuggestion.trim()); // Update Transaction Type field
                        } else {
                            System.err.println("AI returned non-standard category despite prompt: " + finalSuggestion);
                            JOptionPane.showMessageDialog(addDialog, "AI returned an unexpected category format:\n" + finalSuggestion + "\nPlease enter manually.", "AI Result Anomaly", JOptionPane.WARNING_MESSAGE);
                            textFields[1].setText("");
                        }
                    } else if (finalSuggestion != null && finalSuggestion.startsWith("Error:")) {
                        JOptionPane.showMessageDialog(addDialog, "Failed to get AI category suggestion!\n" + finalSuggestion.substring(6), "AI Error", JOptionPane.ERROR_MESSAGE);
                        textFields[1].setText("");
                    } else {
                        JOptionPane.showMessageDialog(addDialog, "AI could not provide a category suggestion.", "AI Tip", JOptionPane.INFORMATION_MESSAGE);
                        textFields[1].setText("");
                    }

                    // 5. Re-enable buttons on EDT
                    aiSuggestButton.setEnabled(true);
                    // confirmButton.setEnabled(true);
                    System.out.println("UI update complete, buttons re-enabled.");
                });
            });

            // 6. Show the modal waiting dialog LAST in the EDT block
            System.out.println("Showing waiting dialog (EDT block continues here).");
            waitingDialog.setLocationRelativeTo(addDialog);
            waitingDialog.setVisible(true); // THIS CALL BLOCKS THE EDT
            System.out.println("waiting dialog is now hidden (EDT unblocked).");
        });


        // Add Confirm button action listener
        confirmButton.addActionListener(e -> {
            // Get values from textFields array + ComboBox
            String transactionTime = textFields[0].getText().trim();
            String finalTransactionType = textFields[1].getText().trim();
            String counterparty = textFields[2].getText().trim();
            String commodity = textFields[3].getText().trim();
            String inOut = (String) inOutComboBox.getSelectedItem();
            String paymentAmountText = textFields[4].getText().trim();
            String paymentMethod = textFields[5].getText().trim();
            String currentStatus = textFields[6].getText().trim();
            String orderNumber = textFields[7].getText().trim(); // Order Number
            String merchantNumber = textFields[8].getText().trim(); // Merchant Number
            String remarks = textFields[9].getText().trim(); // Remarks

            // --- Input Validation ---
            if (orderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "Order Number cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Optional: Validate Order Number uniqueness in Service layer BEFORE adding

            if (finalTransactionType.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "Transaction Type cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate against standard categories for manual input
            if (!StandardCategories.ALL_KNOWN_TYPES.contains(finalTransactionType)) {
                JOptionPane.showMessageDialog(addDialog, "Transaction type must be one of the standard categories!\nAllowed categories:\n" + StandardCategories.getAllCategoriesString(), "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double paymentAmount = 0.0;
            if (!paymentAmountText.isEmpty()) {
                try {
                    paymentAmount = Double.parseDouble(paymentAmountText);
                    if (paymentAmount < 0) {
                        JOptionPane.showMessageDialog(addDialog, "Amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(addDialog, "Amount format is incorrect! Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            // Validate non-negative amount consistency with In/Out
            if (paymentAmount < 0 && (inOut != null && inOut.equals("Income"))) { // Assuming "Income" or "Expense" from ComboBox
                JOptionPane.showMessageDialog(addDialog, "Income amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (paymentAmount < 0 && (inOut != null && inOut.equals("Expense"))) {
                JOptionPane.showMessageDialog(addDialog, "Expense amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            Transaction newTransaction = new Transaction(
                    transactionTime, finalTransactionType, counterparty, commodity, inOut,
                    paymentAmount, paymentMethod, currentStatus, orderNumber, merchantNumber, remarks
            );
            try {
                // Optional: Check for duplicate order number in service layer before adding
                // boolean isUnique = transactionService.isOrderNumberUnique(orderNumber);
                // if (!isUnique) {
                //      JOptionPane.showMessageDialog(addDialog, "Order Number '" + orderNumber + "' already exists!", "Input Error", JOptionPane.ERROR_MESSAGE);
                //      return;
                // }

                transactionService.addTransaction(newTransaction);
                loadCSVDataForCurrentUser("");
                clearSearchFields();
                addDialog.dispose();
                JOptionPane.showMessageDialog(null, "Transaction added successfully!", "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to add transaction!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to add transaction!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> addDialog.dispose());
        addDialog.pack();
        addDialog.setLocationRelativeTo(this);
        addDialog.setVisible(true);
    }

    /**
     * Shows the dialog for editing an existing transaction.
     * @param rowIndex The row index of the transaction in the current table view.
     */
    public void editRow(int rowIndex) {
        System.out.println("Editing row: " + rowIndex + " for user " + currentUser.getUsername());

        // Define JDialog, JPanel, GridBagConstraints at the start
        JDialog editDialog = new JDialog();
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            // Retrieve data from the currently displayed table row
            Vector<String> rowData = new Vector<>();
            for (int i = 0; i <= 10; i++) { // Columns 0 to 10 are Transaction fields
                Object value = this.tableModel.getValueAt(rowIndex, i);
                rowData.add(value != null ? value.toString() : "");
            }
            System.out.println("Retrieved row data from table model for editing: " + rowData);

            String originalOrderNumber = rowData.get(8).trim(); // Order number is at index 8
            if (originalOrderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Cannot edit: Order Number is empty!", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to edit row " + rowIndex + " but order number is empty in the table.");
                return; // Return immediately if no order number in the row
            }

            editDialog.setTitle("Edit Transaction (Order No: " + originalOrderNumber + ")");
            editDialog.setModal(true);

            // Default constraints for components in the dialog panel
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.weightx = 1.0; // Input components get horizontal weight

            // Fields for the dialog (order matches fieldNames)
            JTextField transactionTimeField = new JTextField(rowData.get(0));
            JTextField transactionTypeField = new JTextField(rowData.get(1));
            JButton aiSuggestButton = new JButton("AI Category Suggestion");
            JTextField counterpartyField = new JTextField(rowData.get(2));
            JTextField commodityField = new JTextField(rowData.get(3));
            JComboBox<String> editInOutComboBox = new JComboBox<>(new String[]{"Income", "Expense"}); // JComboBox for In/Out
            // Set initial value for In/Out ComboBox
            String currentInOutValue = rowData.get(4); // In/Out is at index 4 in rowData
            for (int j = 0; j < editInOutComboBox.getItemCount(); j++) {
                if (currentInOutValue != null && currentInOutValue.equalsIgnoreCase(editInOutComboBox.getItemAt(j))) { // Use equalsIgnoreCase
                    editInOutComboBox.setSelectedIndex(j);
                    break;
                }
            }
            JTextField paymentAmountField = new JTextField(rowData.get(5));
            JTextField paymentMethodField = new JTextField(rowData.get(6));
            JTextField currentStatusField = new JTextField(rowData.get(7));
            JTextField orderNumberField = new JTextField(rowData.get(8)); // Order Number field (from rowData)
            JTextField merchantNumberField = new JTextField(rowData.get(9));
            JTextField remarksField = new JTextField(rowData.get(10));

            // Disable Order Number field editing - Order Number is the key, should not be changeable via edit
            orderNumberField.setEditable(false);


            // Add components using GridBagLayout
            String[] fieldNames = {
                    "Transaction Time", "Transaction Type", "Counterparty", "Commodity",
                    "In/Out", "Amount(CNY)", "Payment Method", "Current Status",
                    "Order Number", "Merchant Number", "Remarks"
            };
            // Map field names to the created components for easier iteration
            java.util.Map<String, Component> componentMap = new java.util.LinkedHashMap<>(); // Use LinkedHashMap to maintain order
            componentMap.put("Transaction Time", transactionTimeField);
            componentMap.put("Transaction Type", transactionTypeField);
            componentMap.put("Counterparty", counterpartyField);
            componentMap.put("Commodity", commodityField);
            componentMap.put("In/Out", editInOutComboBox); // This is the ComboBox
            componentMap.put("Amount(CNY)", paymentAmountField);
            componentMap.put("Payment Method", paymentMethodField);
            componentMap.put("Current Status", currentStatusField);
            componentMap.put("Order Number", orderNumberField);
            componentMap.put("Merchant Number", merchantNumberField);
            componentMap.put("Remarks", remarksField);


            int row = 0;
            for (String fieldName : fieldNames) {
                // Constraints for Label (Column 0)
                gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0;
                gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
                dialogPanel.add(new JLabel(fieldName + ":"), gbc);

                // Reset constraints for the input component
                gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.HORIZONTAL;


                if (fieldName.equals("Transaction Type")) { // Row for Transaction Type and AI button
                    gbc.gridwidth = 1; // Field takes 1 column
                    dialogPanel.add(componentMap.get(fieldName), gbc); // Add Transaction Type field

                    // Constraints for AI Suggest Button (Column 2)
                    gbc.gridx = 2; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0;
                    gbc.fill = GridBagConstraints.NONE;
                    dialogPanel.add(aiSuggestButton, gbc);
                    gbc.fill = GridBagConstraints.HORIZONTAL; // Reset fill

                } else { // All other components (JTextFields and JComboBox)
                    gbc.gridwidth = 2; // Component spans 2 columns
                    dialogPanel.add(componentMap.get(fieldName), gbc); // Add the component
                }

                row++; // Move to the next row
            }


            // --- Define the modal waiting dialog for AI suggestion ---
            JDialog waitingDialog = new JDialog(editDialog, "Please wait", true);
            waitingDialog.setLayout(new FlowLayout());
            waitingDialog.add(new JLabel("Getting AI category suggestion..."));
            waitingDialog.setSize(250, 100);
            waitingDialog.setResizable(false);
            waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Prevent closing with X


            // Add Confirm and Cancel buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton confirmButton = new JButton("Confirm");
            JButton cancelButton = new JButton("Cancel");
            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);

            // Constraints for Button Panel (placed below the last row)
            gbc.gridx = 0; gbc.gridy = fieldNames.length; // Row 11 (after 0-10)
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(15, 5, 5, 5);
            gbc.weightx = 0.0; gbc.weighty = 1.0; // Give this row vertical weight
            dialogPanel.add(buttonPanel, gbc);


            editDialog.add(dialogPanel, BorderLayout.CENTER);


            // Add AI Suggest button action listener - Uses ExecutorService
            aiSuggestButton.addActionListener(e -> {
                System.out.println("AI Suggest button clicked (EDT) in edit dialog.");
                aiSuggestButton.setEnabled(false);

                // Build temporary transaction object from dialog components' current values
                Transaction tempTransaction = new Transaction(
                        transactionTimeField.getText().trim(),
                        transactionTypeField.getText().trim(),
                        counterpartyField.getText().trim(),
                        commodityField.getText().trim(),
                        (String) editInOutComboBox.getSelectedItem(), // Get value from the edit dialog's ComboBox
                        safeParseDouble(paymentAmountField.getText().trim()),
                        paymentMethodField.getText().trim(),
                        currentStatusField.getText().trim(),
                        orderNumberField.getText().trim(), // Use the disabled field's text for ON
                        merchantNumberField.getText().trim(),
                        remarksField.getText().trim()
                );
                // Ensure the original order number is used for the temp transaction key if needed by RecognizeTransaction
                // It's already in the tempTransaction from orderNumberField, which holds rowData.get(8)
                // tempTransaction.setOrderNumber(originalOrderNumber); // This line might not be needed if orderNumberField holds the correct value

                // Submit the AI task to the ExecutorService
                executorService.submit(() -> {
                    System.out.println("AI Suggest task submitted to ExecutorService (edit dialog)...");
                    String aiSuggestion = null;
                    try {
                        // Thread.sleep(3000); // Simulate delay
                        aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);
                        System.out.println("AI Suggest task finished (edit dialog). Result: " + aiSuggestion);
                    } catch (Exception ex) {
                        System.err.println("Error in AI Suggest task (edit dialog): " + ex.getMessage());
                        ex.printStackTrace();
                        aiSuggestion = "Error: " + ex.getMessage();
                    }

                    String finalSuggestion = aiSuggestion;
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        System.out.println("Updating UI on EDT after AI Suggest task (edit dialog).");
                        waitingDialog.setVisible(false);

                        if (finalSuggestion != null && !finalSuggestion.isEmpty() && !finalSuggestion.startsWith("Error:")) {
                            if (StandardCategories.ALL_KNOWN_TYPES.contains(finalSuggestion.trim())) {
                                transactionTypeField.setText(finalSuggestion.trim()); // Update Transaction Type field
                            } else {
                                System.err.println("AI returned non-standard category despite prompt (edit dialog): " + finalSuggestion);
                                JOptionPane.showMessageDialog(editDialog, "AI returned an unexpected category format:\n" + finalSuggestion + "\nPlease enter manually.", "AI Result Anomaly", JOptionPane.WARNING_MESSAGE);
                                transactionTypeField.setText("");
                            }
                        } else if (finalSuggestion != null && finalSuggestion.startsWith("Error:")) {
                            JOptionPane.showMessageDialog(editDialog, "Failed to get AI category suggestion!\n" + finalSuggestion.substring(6), "AI Error", JOptionPane.ERROR_MESSAGE);
                            transactionTypeField.setText("");
                        } else {
                            JOptionPane.showMessageDialog(editDialog, "AI could not provide a category suggestion.", "AI Tip", JOptionPane.INFORMATION_MESSAGE);
                            transactionTypeField.setText("");
                        }

                        aiSuggestButton.setEnabled(true);
                        System.out.println("UI update complete, buttons re-enabled (edit dialog).");
                    });
                });

                System.out.println("Showing waiting dialog (EDT block continues here in edit dialog).");
                waitingDialog.setLocationRelativeTo(editDialog);
                waitingDialog.setVisible(true); // THIS CALL BLOCKS THE EDT
                System.out.println("waiting dialog is now hidden (EDT unblocked in edit dialog).");
            });


            // Add Confirm button action listener
            confirmButton.addActionListener(e -> {
                // Get values from dialog components
                String transactionTime = transactionTimeField.getText().trim();
                String finalTransactionType = transactionTypeField.getText().trim();
                String counterparty = counterpartyField.getText().trim();
                String commodity = commodityField.getText().trim();
                String inOut = (String) editInOutComboBox.getSelectedItem(); // GET VALUE FROM THE EDIT DIALOG'S COMBOBOX
                String paymentAmountText = paymentAmountField.getText().trim();
                String paymentMethod = paymentMethodField.getText().trim();
                String currentStatus = currentStatusField.getText().trim();
                String orderNumber = orderNumberField.getText().trim(); // Get from disabled field, which holds original ON
                String merchantNumber = merchantNumberField.getText().trim();
                String remarks = remarksField.getText().trim();

                // --- Input Validation ---
                // Order Number field is disabled, it should contain the originalOrderNumber.
                // Validation on originalOrderNumber already happened at method entry.

                if (finalTransactionType.isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog, "Transaction Type cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Validate against standard categories
                if (!StandardCategories.ALL_KNOWN_TYPES.contains(finalTransactionType)) {
                    JOptionPane.showMessageDialog(editDialog, "Transaction type must be one of the standard categories!\nAllowed categories:\n" + StandardCategories.getAllCategoriesString(), "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Validate In/Out is one of expected values (ComboBox ensures this for "Income"/"Expense")
                if (!inOut.equals("Income") && !inOut.equals("Expense")) { // ComboBox only has these two, but add check for robustness
                    JOptionPane.showMessageDialog(editDialog, "In/Out field must be 'Income' or 'Expense'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double paymentAmount = 0.0;
                if (!paymentAmountText.isEmpty()) {
                    try {
                        paymentAmount = Double.parseDouble(paymentAmountText);
                        if (paymentAmount < 0) {
                            JOptionPane.showMessageDialog(editDialog, "Amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(editDialog, "Amount format is incorrect! Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                // Check for non-negative amount consistency with In/Out
                if (paymentAmount < 0 && inOut.equals("Income")) {
                    JOptionPane.showMessageDialog(editDialog, "Income amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (paymentAmount < 0 && inOut.equals("Expense")) {
                    JOptionPane.showMessageDialog(editDialog, "Expense amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Transaction updatedTransaction = new Transaction(
                        transactionTime, finalTransactionType, counterparty, commodity, inOut,
                        paymentAmount, paymentMethod, currentStatus, originalOrderNumber, // Use the correct originalOrderNumber (captured at method start)
                        merchantNumber, remarks
                );


                try {
                    transactionService.changeTransaction(updatedTransaction);
                    System.out.println("Edit successful. Preparing to refresh display filtered by InOut: " + updatedTransaction.getInOut());
                    clearSearchFields();
                    String updatedInOut = updatedTransaction.getInOut();
                    boolean foundInOut = false;
                    for(int i=0; i < searchInOutComboBox.getItemCount(); i++) {
                        if (updatedInOut != null && updatedInOut.equals(searchInOutComboBox.getItemAt(i))) {
                            searchInOutComboBox.setSelectedItem(updatedInOut);
                            foundInOut = true;
                            break;
                        }
                    }
                    if (!foundInOut) { searchInOutComboBox.setSelectedItem(""); }

                    triggerCurrentSearch();

                    editDialog.dispose();
                    JOptionPane.showMessageDialog(null, "Update successful!", "Information", JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Edited row " + rowIndex + " for order number " + originalOrderNumber + " and refreshed display.");

                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(editDialog, "Update failed!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(editDialog, "Update failed!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            cancelButton.addActionListener(e -> editDialog.dispose());

            // --- Dialog setup and showing ---
            editDialog.pack();
            editDialog.setLocationRelativeTo(this);
            editDialog.setVisible(true); // Show the edit dialog
            // --- End Dialog setup ---

        } else {
            // This block is for invalid rowIndex.
            System.err.println("Attempted to edit row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    /**
     * Creates the panel for displaying AI analysis results and controls.
     * @return The AI analysis panel.
     */
    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel for General AI Analysis controls
        JPanel generalAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField userRequestField = new JTextField(40);
        aiStartTimeField = new JTextField(10);
        aiEndTimeField = new JTextField(10);
        aiAnalyzeButton = new JButton("General Analysis"); // Updated text


        generalAnalysisPanel.add(new JLabel("General Analysis Request:"));
        generalAnalysisPanel.add(userRequestField);
        generalAnalysisPanel.add(new JLabel("Time Range (yyyy/MM/dd HH:mm): From:"));
        generalAnalysisPanel.add(aiStartTimeField);
        generalAnalysisPanel.add(new JLabel("To:"));
        generalAnalysisPanel.add(aiEndTimeField);
        generalAnalysisPanel.add(aiAnalyzeButton);


        // Panel for Summary-Based AI Analysis controls
        JPanel summaryAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiPersonalSummaryButton = new JButton("Personal Spending Summary");
        aiSavingsGoalsButton = new JButton("Savings Goal Suggestions");
        aiPersonalSavingTipsButton = new JButton("Personalized Saving Tips");

        summaryAnalysisPanel.add(new JLabel("Based on Monthly Summary Analysis:"));
        summaryAnalysisPanel.add(aiPersonalSummaryButton);
        summaryAnalysisPanel.add(aiSavingsGoalsButton);
        summaryAnalysisPanel.add(aiPersonalSavingTipsButton);


        // Panel for Student-Specific features
        JPanel csButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiBudgetButton = new JButton("Budget Suggestion (Student)");
        aiTipsButton = new JButton("Saving Tips (Student)");
        csButtonsPanel.add(new JLabel("Student-Specific Features:"));
        csButtonsPanel.add(aiBudgetButton);
        csButtonsPanel.add(aiTipsButton);


        // --- NEW Panel for Batch/Debug Tasks ---
        JPanel batchTaskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        runBatchAiButton = new JButton("Run Batch AI Analysis (Test ExecutorService)"); // Create the button
        batchTaskPanel.add(new JLabel("Multi-thread performace testing: ")); // Optional label
        batchTaskPanel.add(runBatchAiButton); // Add the button
        // --- End New Panel ---

        // Combine all control panels in a box layout at the top
        JPanel topControlPanel = new JPanel();
        topControlPanel.setLayout(new BoxLayout(topControlPanel, BoxLayout.Y_AXIS));
        topControlPanel.add(generalAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(summaryAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(csButtonsPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(batchTaskPanel); // Add the new batch task panel
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing below it


        panel.add(topControlPanel, BorderLayout.NORTH);

        // Center area for displaying AI results
        aiResultArea = new JTextArea();
        aiResultArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // Using a common font name
        aiResultArea.setLineWrap(true);
        aiResultArea.setWrapStyleWord(true);
        aiResultArea.setEditable(false);
        aiResultArea.setText("Welcome to the AI Personal Finance Analysis feature.\n\n" +
                "You can try the following operations:\n" +
                "1. Enter a general analysis request in the input field above (based on raw data, time range can be specified), then click \"General Analysis\".\n" +
                "2. Click \"Personal Spending Summary\" to get a detailed summary based on your monthly income and expenses.\n" +
                "3. Click \"Savings Goal Suggestions\" to get savings advice based on your income and expenditure situation.\n" +
                "4. Click \"Personalized Saving Tips\" to get saving advice based on your spending categories.\n" +
                "5. Student users can click \"Budget Suggestion (Student)\" and \"Saving Tips (Student)\" for exclusive advice.\n");


        JScrollPane resultScrollPane = new JScrollPane(aiResultArea);
        panel.add(resultScrollPane, BorderLayout.CENTER);

        // --- Action Listener for the NEW Batch Button ---
        runBatchAiButton.addActionListener(e -> {
            int numberOfTasks = 30; // Define the number of tasks for the batch run
            String userRequest = "Summarize recent activity"; // Example request for batch tasks
            String filePath = currentUser.getTransactionFilePath(); // Use current user's file path
            String startTime = "2024/01/01"; // Example time range for batch tasks
            String endTime = ""; // Example time range

            System.out.println("Starting batch AI analysis (" + numberOfTasks + " tasks) via ExecutorService...");

            // Clear previous results and show a loading message
            aiResultArea.setText("Running batch AI analysis (" + numberOfTasks + " tasks), please wait...\n");
            // Disable all AI related buttons while batch is running
            setAIButtonsEnabled(false);

            // Use an AtomicInteger to track completed tasks across threads
            java.util.concurrent.atomic.AtomicInteger completedTasks = new java.util.concurrent.atomic.AtomicInteger(0);
            long startTimeMillis = System.currentTimeMillis(); // Record start time for total duration

            for (int i = 0; i < numberOfTasks; i++) {
                final int taskIndex = i;
                // Submit each individual task to the ExecutorService
                executorService.submit(() -> {
                    String taskResult = "Task " + (taskIndex + 1) + " failed."; // Default error message for this specific task
                    boolean success = false;
                    try {
                        // Call the AI service method for analysis (example)
                        // In a real batch test, you might call analyzeTransactions or a simpler task
                        String result = aiTransactionService.analyzeTransactions(userRequest + " (Task " + (taskIndex + 1) + ")", filePath, startTime, endTime);
                        taskResult = "Task " + (taskIndex + 1) + " completed: " + result.substring(0, Math.min(result.length(), 50)) + "..."; // Truncate result for log
                        success = true;
                        System.out.println(taskResult);
                    } catch (Exception ex) {
                        System.err.println("Task " + (taskIndex + 1) + " failed: " + ex.getMessage());
                        ex.printStackTrace();
                        taskResult = "Task " + (taskIndex + 1) + " failed: " + ex.getMessage();
                    } finally {
                        // Increment completed task count
                        int doneCount = completedTasks.incrementAndGet();

                        // Update UI with progress and final status on EDT
                        SwingUtilities.invokeLater(() -> {
                            // Append status for this task
                            // aiResultArea.append(taskResult + "\n"); // Appending can make UI jumpy for large batches

                            // Display overall progress instead of appending every result
                            if (doneCount < numberOfTasks) {
                                aiResultArea.setText("Running batch AI analysis... " + doneCount + "/" + numberOfTasks + " tasks completed.");
                            } else {
                                // All tasks are done
                                long endTimeMillis = System.currentTimeMillis();
                                long totalTimeSeconds = (endTimeMillis - startTimeMillis) / 1000;
                                aiResultArea.setText("Batch run completed!\n" + numberOfTasks + " tasks finished in " + totalTimeSeconds + " seconds.");

                                // Re-enable buttons
                                setAIButtonsEnabled(true);
                            }
                        });
                    }
                });
            }
        });
        // Personal Spending Summary Button
        aiPersonalSummaryButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personal Spending Summary ---\n\nGenerating summary based on your monthly spending data, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String result = aiTransactionService.generatePersonalSummary(currentUser.getTransactionFilePath()); // Call the new method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Personal Spending Summary ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // Savings Goal Suggestions Button
        aiSavingsGoalsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Savings Goal Suggestions ---\n\nGenerating savings goal suggestions based on your income and expenses, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String result = aiTransactionService.suggestSavingsGoals(currentUser.getTransactionFilePath()); // Call the new method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Savings Goal Suggestions ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // Personalized Saving Tips Button
        aiPersonalSavingTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personalized Saving Tips ---\n\nGenerating saving tips based on your spending categories, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String result = aiTransactionService.givePersonalSavingTips(currentUser.getTransactionFilePath()); // Call the new method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Personalized Saving Tips ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // General Analysis Button
        aiAnalyzeButton.addActionListener(e -> {
            String userRequest = userRequestField.getText().trim();
            String startTimeStr = aiStartTimeField.getText().trim();
            String endTimeStr = aiEndTimeField.getText().trim();

            if (userRequest.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the AI general analysis request.", "Input Tip", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (startTimeStr.isEmpty() && !endTimeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter at least the start time for the analysis.", "Input Tip", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            aiResultArea.setText("--- Generating General Analysis ---\n\n" + "Performing AI general analysis, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String result = aiTransactionService.analyzeTransactions(userRequest, currentUser.getTransactionFilePath(), startTimeStr, endTimeStr);
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- General Analysis Result ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // College Student Budget Button
        aiBudgetButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Budget Suggestion ---\n\nGenerating budget suggestion based on your historical spending, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String resultMessage;
                try {
                    double[] budgetRange = collegeStudentNeeds.generateBudget(currentUser.getTransactionFilePath());
                    if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] != -1) {
                        resultMessage = String.format("Based on your spending records, the recommended budget range for next week is: [%.2f CNY, %.2f CNY]", budgetRange[0], budgetRange[1]);
                    } else if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] == -1) {
                        resultMessage = "Not enough spending records to calculate weekly budget suggestions.";
                    } else {
                        resultMessage = "Failed to generate budget suggestion, AI did not return a valid range.";
                        System.err.println("AI Budget generation failed, invalid response format.");
                    }
                } catch (Exception ex) {
                    resultMessage = "Failed to generate budget suggestion!\n" + ex.getMessage();
                    System.err.println("Error generating AI budget:");
                    ex.printStackTrace();
                }
                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Student Budget Suggestion ---\n\n" + finalResultMessage);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // College Student Tips Button
        aiTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Saving Tips ---\n\nGenerating saving tips, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String resultMessage;
                try {
                    resultMessage = collegeStudentNeeds.generateTipsForSaving(currentUser.getTransactionFilePath());
                } catch (Exception ex) {
                    resultMessage = "Failed to generate saving tips!\n" + ex.getMessage();
                    System.err.println("Error generating AI tips:");
                    ex.printStackTrace();
                }
                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Student Saving Tips ---\n\n" + finalResultMessage);
                    setAIButtonsEnabled(true);
                });
            });
        });
        return panel;
    }

    /**
     * Helper method to enable or disable all AI-related buttons (Updated).
     * Includes the new batch button.
     *
     * @param enabled True to enable, false to disable.
     */
    private void setAIButtonsEnabled(boolean enabled) {
        if (aiAnalyzeButton != null) aiAnalyzeButton.setEnabled(enabled);
        if (aiBudgetButton != null) aiBudgetButton.setEnabled(enabled);
        if (aiTipsButton != null) aiTipsButton.setEnabled(enabled);
        if (aiPersonalSummaryButton != null) aiPersonalSummaryButton.setEnabled(enabled);
        if (aiSavingsGoalsButton != null) aiSavingsGoalsButton.setEnabled(enabled);
        if (aiPersonalSavingTipsButton != null) aiPersonalSavingTipsButton.setEnabled(enabled);
        if (runBatchAiButton != null) runBatchAiButton.setEnabled(enabled); // Include the new button
    }

    /**
     * Creates the panel for displaying admin statistics.
     * @return The admin stats panel.
     */
    private JPanel createAdminStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateStatsButton = new JButton("Generate/Update Statistics");
        refreshDisplayButton = new JButton("Refresh Display");
        controlPanel.add(generateStatsButton);
        controlPanel.add(refreshDisplayButton);
        panel.add(controlPanel, BorderLayout.NORTH);

        adminStatsArea = new JTextArea();
        adminStatsArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        adminStatsArea.setEditable(false);
        adminStatsArea.setLineWrap(true);
        adminStatsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(adminStatsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Generate Stats button listener (Uses ExecutorService)
        generateStatsButton.addActionListener(e -> {
            adminStatsArea.setText("Generating/Updating summary statistics, please wait...\n");
            generateStatsButton.setEnabled(false);
            refreshDisplayButton.setEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> {
                String message;
                try {
                    summaryStatisticService.generateAndSaveWeeklyStatistics();
                    message = "Summary statistics generated/updated successfully!\nPlease click 'Refresh Display' to view the latest data.";
                    System.out.println("Generate Stats task finished: " + message);
                } catch (Exception ex) {
                    message = "Failed to generate/update summary statistics!\n" + ex.getMessage();
                    System.err.println("Generate Stats task failed: " + ex.getMessage());
                    ex.printStackTrace();
                }

                String finalMessage = message;
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    adminStatsArea.setText(finalMessage);
                    generateStatsButton.setEnabled(true);
                    refreshDisplayButton.setEnabled(true);
                });
            });
        });

        // Refresh Display button listener (Uses ExecutorService)
        refreshDisplayButton.addActionListener(e -> {
            displaySummaryStatistics(); // This method itself submits a task
        });

        // Initial display when the panel is first shown (Optional - can be triggered by refreshPanelData if implemented)
        // For admin stats, it's good to load existing data on panel creation/display.
        // Submit load task to ExecutorService
        executorService.submit(() -> { // Use submit
            System.out.println("Initial Admin Stats load task submitted to ExecutorService...");
            SwingUtilities.invokeLater(() -> adminStatsArea.setText("Loading existing statistics...\n"));
            try {
                // This loads data from the configured summary file path (likely admin's own or global)
                List<SummaryStatistic> initialStats = summaryStatisticService.getAllSummaryStatistics();
                System.out.println("Initial Admin Stats load task finished. Found " + initialStats.size() + " stats.");
                if (!initialStats.isEmpty()) {
                    // If initial stats exist, trigger display (which submits another EDT task)
                    SwingUtilities.invokeLater(this::displaySummaryStatistics); // This triggers display using the loaded data (indirectly)
                } else {
                    SwingUtilities.invokeLater(() -> adminStatsArea.setText("No existing summary statistics found.\nPlease click the 'Generate/Update Statistics' button to generate them."));
                }
            } catch (IOException ex) {
                System.err.println("Initial Admin Stats load task failed: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> adminStatsArea.setText("Failed to load existing statistics!\n" + ex.getMessage()));
            }
        });

        return panel;
    }

    /**
     * Displays the summary statistics in the admin stats text area.
     * This method submits the data loading to the ExecutorService.
     */
    private void displaySummaryStatistics() {
        adminStatsArea.setText("Loading summary statistics...\n");
        if(generateStatsButton != null) generateStatsButton.setEnabled(false);
        if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(false);

        // Submit data loading task to ExecutorService
        executorService.submit(() -> { // Use submit
            String displayContent;
            try {
                // This loads data from the configured summary file path (likely admin's own or global)
                List<SummaryStatistic> stats = summaryStatisticService.getAllSummaryStatistics();
                if (stats.isEmpty()) {
                    displayContent = "No summary statistics currently available.\nPlease click the 'Generate/Update Statistics' button first.";
                } else {
                    StringBuilder sb = new StringBuilder("===== Summary Statistics =====\n\n");
                    stats.sort(Comparator.comparing(SummaryStatistic::getWeekIdentifier));
                    for (int i = stats.size() - 1; i >= 0; i--) {
                        SummaryStatistic stat = stats.get(i);
                        sb.append("Week Identifier: ").append(stat.getWeekIdentifier()).append("\n");
                        // NOTE: These fields were originally named "total_income_all_users" etc.
                        // If the SummaryStatistic model was updated to single-user stats, these getters need adjustment
                        // or the model names should reflect 'Total Income (User)' etc.
                        // Assuming SummaryStatistic model now has simple totalIncome/totalExpense etc. for a single user:
                        sb.append("  Total Income (User): ").append(String.format("%.2f", stat.getTotalIncomeAllUsers())).append(" CNY\n"); // Getter name might be outdated
                        sb.append("  Total Expense (User): ").append(String.format("%.2f", stat.getTotalExpenseAllUsers())).append(" CNY\n"); // Getter name might be outdated
                        if (stat.getTopExpenseCategoryAmount() > 0) {
                            sb.append("  Top Expense Category: ").append(stat.getTopExpenseCategory()).append(" (").append(String.format("%.2f", stat.getTopExpenseCategoryAmount())).append(" CNY)\n");
                        } else {
                            sb.append("  Top Expense Category: No significant expense category\n");
                        }
                        // If SummaryStatistic model removed numberOfUsersWithTransactions, remove this line
                        sb.append("  Number of Participating Users: ").append(stat.getNumberOfUsersWithTransactions()).append("\n"); // Getter name might be outdated
                        sb.append("  Generated Time: ").append(stat.getTimestampGenerated()).append("\n");
                        sb.append("--------------------\n");
                    }
                    displayContent = sb.toString();
                }
            } catch (IOException ex) {
                displayContent = "Failed to load summary statistics!\n" + ex.getMessage();
                ex.printStackTrace();
            }
            String finalDisplayContent = displayContent;
            SwingUtilities.invokeLater(() -> { // Update UI on EDT
                adminStatsArea.setText(finalDisplayContent);
                if(generateStatsButton != null) generateStatsButton.setEnabled(true);
                if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(true);
            });
        });
    }


    /**
     * Handles the deletion of a transaction row from the table and the underlying data.
     * @param rowIndex The index of the row to delete in the current table view.
     */
    public void deleteRow(int rowIndex) {
        System.out.println("Attempting to delete row: " + rowIndex + " for user " + currentUser.getUsername());
        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            String orderNumber = (String) this.tableModel.getValueAt(rowIndex, 8); // OrderNumber is at index 8
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Cannot delete: Order Number is empty!", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to delete row " + rowIndex + " but order number is null or empty.");
                return;
            }
            orderNumber = orderNumber.trim();
            System.out.println("Deleting transaction with order number: " + orderNumber);
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete the transaction with order number '" + orderNumber + "'?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // Submit deletion task to the ExecutorService
                String finalOrderNumber = orderNumber; // Final variable for lambda
                executorService.submit(() -> { // Use submit
                    System.out.println("Delete task submitted to ExecutorService for ON: " + finalOrderNumber);
                    String message;
                    boolean deleted = false;
                    try {
                        deleted = transactionService.deleteTransaction(finalOrderNumber);
                        if (deleted) {
                            message = "Delete successful!";
                            System.out.println("Delete task finished: " + message);
                        } else {
                            message = "Delete failed: Corresponding order number " + finalOrderNumber + " not found.";
                            System.err.println("Delete task failed: " + message);
                        }
                    } catch (Exception ex) {
                        message = "Delete failed!\n" + ex.getMessage();
                        System.err.println("Delete task failed: " + ex.getMessage());
                        ex.printStackTrace();
                    }

                    String finalMessage = message;
                    boolean finalDeleted = deleted; // Final variable for lambda
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        System.out.println("Updating UI on EDT after Delete task.");
                        if (finalDeleted) {
                            // Remove the row from the table model directly if deletion was successful
                            // This is faster than reloading all data.
                            // Note: This assumes the tableModel perfectly reflects the underlying data filtered by the current search.
                            // If the search criteria is complex, reloading might be safer, but slower.
                            // Let's stick with removing the row for performance.
                            // Find the row index again, as it might have changed if multiple deletes happened while task was running.
                            // A safer way would be to pass the original Transaction object or its key.
                            // For simplicity now, let's assume rowIndex hasn't changed, but be aware this is a potential bug area in concurrent updates.
                            // If you want robust concurrent UI updates, reloading after modification/deletion is safer.
                            // Let's stick with removing by index for now, assuming minimal concurrent modification.
                            int currentRowIndex = -1;
                            for(int i=0; i < this.tableModel.getRowCount(); i++) {
                                if (finalOrderNumber.equals(((String) this.tableModel.getValueAt(i, 8)).trim())) {
                                    currentRowIndex = i;
                                    break;
                                }
                            }
                            if (currentRowIndex != -1) {
                                this.tableModel.removeRow(currentRowIndex);
                            } else {
                                // If row wasn't found in the model, just reload all data to be safe.
                                loadCSVDataForCurrentUser(""); // Reload all or trigger current search
                            }


                            JOptionPane.showMessageDialog(null, finalMessage, "Information", JOptionPane.INFORMATION_MESSAGE);
                            System.out.println("UI update complete after Delete task. Row removed.");

                            // After deletion, trigger a refresh of the displayed data based on current search criteria.
                            triggerCurrentSearch(); // Trigger refresh after UI update
                        } else {
                            // If deletion failed in service (e.g., not found), just show message.
                            JOptionPane.showMessageDialog(null, finalMessage, "Error", JOptionPane.ERROR_MESSAGE);
                            System.out.println("UI update complete after Delete task. Deletion failed.");
                        }
                    });
                });


            } else {
                System.out.println("Deletion cancelled by user for order number: " + orderNumber);
            }
        } else {
            System.err.println("Attempted to delete row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    /**
     * Creates a Vector representing a table row from a Transaction object.
     * @param transaction The Transaction object.
     * @return A Vector<String> representing the table row.
     */
    private Vector<String> createRowFromTransaction(Transaction transaction) {
        Vector<String> row = new Vector<>();
        row.add(emptyIfNull(transaction.getTransactionTime()));
        row.add(emptyIfNull(transaction.getTransactionType()));
        row.add(emptyIfNull(transaction.getCounterparty()));
        row.add(emptyIfNull(transaction.getCommodity()));
        row.add(emptyIfNull(transaction.getInOut()));
        row.add(String.valueOf(transaction.getPaymentAmount())); // Convert double to String
        row.add(emptyIfNull(transaction.getPaymentMethod()));
        row.add(emptyIfNull(transaction.getCurrentStatus()));
        row.add(emptyIfNull(transaction.getOrderNumber()));
        row.add(emptyIfNull(transaction.getMerchantNumber()));
        row.add(emptyIfNull(transaction.getRemarks()));
        row.add("Modify"); // Button text
        row.add("Delete"); // Button text
        return row;
    }

    /**
     * Handles the search operation based on input panel criteria and updates the table.
     * @param query1 Transaction Time criteria.
     * @param query2 Transaction Type criteria.
     * @param query3 Counterparty criteria.
     * @param query4 Commodity criteria.
     * @param query6 In/Out criteria.
     * @param query5 Payment Method criteria.
     */
    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) {
        System.out.println("Searching with criteria: time='" + query1 + "', type='" + query2 + "', counterparty='" + query3 + "', commodity='" + query4 + "', inOut='" + query6 + "', paymentMethod='" + query5 + "'");
        this.tableModel.setRowCount(0); // Clear the current table display

        Transaction searchCriteria = new Transaction(
                query1, query2, query3, query4, query6,
                0, // Amount is not a search criteria from the UI input fields
                query5,
                "", "", "", "" // Other fields are not searchable from the UI input fields
        );

        // Submit search task to the ExecutorService
        executorService.submit(() -> { // Use submit
            System.out.println("Search task submitted to ExecutorService.");
            try {
                List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);
                System.out.println("Search task finished. Found " + transactions.size() + " results.");

                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    System.out.println("Updating UI on EDT after Search task.");
                    for (Transaction transaction : transactions) {
                        Vector<String> row = createRowFromTransaction(transaction);
                        this.tableModel.addRow(row); // Add matching rows to the table model
                    }
                    System.out.println("UI update complete after Search task. Table refreshed.");
                });

            } catch (Exception ex) {
                System.err.println("Search task failed: " + ex.getMessage());
                ex.printStackTrace();
                String errorMessage = "Search failed!\n" + ex.getMessage();
                SwingUtilities.invokeLater(() -> { // Show error message on EDT
                    JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    System.out.println("UI update complete after Search task. Error message shown.");
                });
            }
        });
    }


    /**
     * Helper method to safely parse a double from a string, returning 0.0 on error.
     * @param value The string to parse.
     * @return The parsed double value, or 0.0 if parsing fails.
     */
    private double safeParseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { System.err.println("Failed to parse double from string: '" + value + "'"); return 0.0; }
    }

    /**
     * Clears the search input fields in the input panel.
     */
    private void clearSearchFields() {
        searchTransactionTimeField.setText("");
        searchTransactionTypeField.setText("");
        searchCounterpartyField.setText("");
        searchCommodityField.setText("");
        searchInOutComboBox.setSelectedItem("");
        searchPaymentMethodField.setText("");
        System.out.println("Cleared search fields.");
    }

    /**
     * Triggers a search operation based on the current values in the search input fields.
     */
    private void triggerCurrentSearch() {
        searchData(
                searchTransactionTimeField.getText().trim(),
                searchTransactionTypeField.getText().trim(),
                searchCounterpartyField.getText().trim(),
                searchCommodityField.getText().trim(),
                (String) searchInOutComboBox.getSelectedItem(),
                searchPaymentMethodField.getText().trim()
        );
        System.out.println("Triggered search with current field values.");
    }

    /**
     * Helper method to return an empty string if the input value is null.
     * @param value The input string.
     * @return The input string if not null, otherwise an empty string.
     */
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns the JTable component. Used primarily for testing purposes.
     * @return The transaction JTable.
     */
    public JTable getTable() {
        return table;
    }

    // Inner classes for ButtonRenderer and ButtonEditor
    // (Paste your ButtonRenderer and ButtonEditor classes here)
    // You will need to ensure these classes are defined within or accessible to MenuUI.

    // Assuming ButtonRenderer class definition is here or in a separate file
    // For example:
    // class ButtonRenderer extends DefaultTableCellRenderer { ... }

    // Assuming ButtonEditor class definition is here or in a separate file
    // For example:
    // class ButtonEditor extends AbstractCellEditor implements TableCellEditor { ... }
    // Note: ButtonEditor constructor takes a MenuUI instance: new ButtonEditor(this)
}

// Make sure your ButtonRenderer and ButtonEditor class definitions are available to MenuUI.
// If they are in separate files, ensure they are in the correct package (Controller) and imported.
// If they were inner classes before, keep them as inner classes or move them to their own files.


// Assuming ButtonRenderer and ButtonEditor are defined here or in separate files in the Controller package.
// If they are inner classes, their definition should be inside the MenuUI class above.
// If they are separate classes, they should be in the Controller package and need imports in MenuUI.
// For now, I'll assume they are in separate files in the same package based on your initial source tree structure.

// You need to provide the full code for ButtonRenderer.java and ButtonEditor.java separately
// if they are not inner classes and their content has changed (e.g., translated strings).
// Based on the source tree, they appear to be separate files.