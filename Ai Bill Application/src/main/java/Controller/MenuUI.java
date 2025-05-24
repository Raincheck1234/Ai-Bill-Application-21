package Controller;

import Constants.StandardCategories; // Import StandardCategories if needed in UI
import Service.AIservice.AITransactionService; // Import AI services
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.SummaryStatisticService; // Import SummaryStatisticService
import Service.TransactionService;
import model.SummaryStatistic; // Import SummaryStatistic
import model.Transaction;
import model.User;
// import Constants.StandardCategories; // Already imported above

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Comparator; // For sorting stats display
import java.util.stream.Collectors; // Added for loadCSVDataForCurrentUser

public class MenuUI extends JPanel { // Extend JPanel for easier use in Main (optional but common)

    private final User currentUser;
    private final TransactionService transactionService;
    private final SummaryStatisticService summaryStatisticService;
    private final AITransactionService aiTransactionService;
    private final CollegeStudentNeeds collegeStudentNeeds;

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
    // REMOVED: private HistogramPanelContainer histogramPanelContainer; // No longer needed

    private JPanel rightPanel;
    private CardLayout cardLayout;

    // UI components for AI panel (existing)
    private JTextArea aiResultArea;
    private JTextField aiStartTimeField;
    private JTextField aiEndTimeField;
    private JButton aiAnalyzeButton;
    private JButton aiBudgetButton;
    private JButton aiTipsButton;
    private JButton aiPersonalSummaryButton;
    private JButton aiSavingsGoalsButton;
    private JButton aiPersonalSavingTipsButton;


    // UI components for Admin Stats panel (existing)
    private JTextArea adminStatsArea;
    private JButton generateStatsButton;
    private JButton refreshDisplayButton;

    // New panel for Visualization
    private VisualizationPanel visualizationPanel; // Add instance field


    // Constructor now accepts all necessary service instances (same as before)
    public MenuUI(User authenticatedUser, TransactionService transactionService,
                  SummaryStatisticService summaryStatisticService,
                  AITransactionService aiTransactionService,
                  CollegeStudentNeeds collegeStudentNeeds) {

        this.currentUser = authenticatedUser;
        this.transactionService = transactionService;
        this.summaryStatisticService = summaryStatisticService;
        this.aiTransactionService = aiTransactionService;
        this.collegeStudentNeeds = collegeStudentNeeds;

        // Initialize table model (same as before)
        String[] columnNames = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks", "Modify", "Delete"};
        this.tableModel = new DefaultTableModel(columnNames, 0);
        this.table = new JTable(this.tableModel);

        // Initialize the main panel layout (same as before)
        setLayout(new BorderLayout());

        // Add the left panel (will be modified)
        add(createLeftPanel(), BorderLayout.WEST);

        // Add the right panel (will be modified)
        setupRightPanel();
        add(rightPanel, BorderLayout.CENTER);

        // Initial data load is done in createMainPanel (same as before)
        // loadCSVDataForCurrentUser("Income");

        System.out.println("MenuUI initialized for user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
    }

    public JPanel createMainPanel() {
        // MenuUI itself is the main panel, just add the initial data
        loadCSVDataForCurrentUser(""); // Load and display all data initially, or a default like "Income"
        return this; // Return itself
    }


    // Method to load CSV data for the current user with optional initial filter
    // Same logic as before
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
                // Assuming "Income" maps to "收" and "Expense" maps to "支" or their English equivalents if data uses that.
                // The UI might use "Income"/"Expense", while data might use "收"/"支" or "In"/"Out".
                // This filter needs to be robust to these variations.
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
            JOptionPane.showMessageDialog(null, "Failed to load user transaction data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to create the left panel (Menu/AI/Admin/Visualization buttons) - MODIFIED
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton menuButton = new JButton("Transaction List"); // "交易列表"
        JButton aiButton = new JButton("AI Analysis");       // "AI分析"
        JButton adminButton = new JButton("Admin Stats");     // "管理员统计"
        JButton visualizationButton = new JButton("Visualization"); // "可视化"

        // Set consistent size for buttons
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

        // Add Admin button only if the user is admin (same as before)
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            leftPanel.add(adminButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Add Visualization button (visible for all users)
        leftPanel.add(visualizationButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        // Add action listeners (existing for Menu, AI, Admin)
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table");
            loadCSVDataForCurrentUser("Income"); // Load "Income" by default or "" for all
        });

        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "AI");
        });

        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            adminButton.addActionListener(e -> {
                cardLayout.show(rightPanel, "AdminStats");
                displaySummaryStatistics(); // Refresh stats display when switching
            });
        }

        // Add action listener for Visualization button - NEW
        visualizationButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Visualization"); // Switch to visualization view
            if (visualizationPanel != null) { // Ensure panel is initialized
                visualizationPanel.refreshPanelData(); // Call refresh method
            }
        });


        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    // Method to set up the right panel, adding different views - MODIFIED
    private void setupRightPanel() {
        this.cardLayout = new CardLayout();
        this.rightPanel = new JPanel(this.cardLayout);

        // Create and add different panels (views)
        JPanel tablePanel = createTablePanel(); // Table view
        JPanel aiPanel = createAIPanel(); // AI view
        JPanel adminStatsPanel = createAdminStatsPanel(); // Admin stats view
        this.visualizationPanel = new VisualizationPanel(this.transactionService);


        rightPanel.add(tablePanel, "Table");
        rightPanel.add(aiPanel, "AI");
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            rightPanel.add(adminStatsPanel, "AdminStats");
        }
        rightPanel.add(visualizationPanel, "Visualization");


        // Set the initially visible card (Table view)
        cardLayout.show(rightPanel, "Table");
    }

    // Method to create the table panel - same as before
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        JPanel inputPanel = createInputPanel();
        tablePanel.add(inputPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(this.table);
        tableScrollPane.setPreferredSize(new Dimension(1000, 250));
        this.table.setFillsViewportHeight(true);
        this.table.setRowHeight(30);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Set cell renderers and editors
        this.table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(this));

        this.table.getColumnModel().getColumn(12).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(12).setCellEditor(new ButtonEditor(this));

        return tablePanel;
    }

    // Inside MenuUI class
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        searchTransactionTimeField = new JTextField(10);
        searchTransactionTypeField = new JTextField(10);
        searchCounterpartyField = new JTextField(10);
        searchCommodityField = new JTextField(10);
        searchInOutComboBox = new JComboBox<>(new String[]{"", "Income", "Expense"}); // "Income", "Expense"
        searchPaymentMethodField = new JTextField(10);

        inputPanel.add(new JLabel("Transaction Time:")); inputPanel.add(searchTransactionTimeField);
        inputPanel.add(new JLabel("Transaction Type:")); inputPanel.add(searchTransactionTypeField);
        inputPanel.add(new JLabel("Counterparty:")); inputPanel.add(searchCounterpartyField);
        inputPanel.add(new JLabel("Commodity:")); inputPanel.add(searchCommodityField);
        inputPanel.add(new JLabel("In/Out:")); inputPanel.add(searchInOutComboBox);
        inputPanel.add(new JLabel("Payment Method:")); inputPanel.add(searchPaymentMethodField);

        searchButton = new JButton("Search");
        JButton addButton = new JButton("Add");
        JButton importButton = new JButton("Import CSV"); // "Import CSV"

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

    // Inside MenuUI class
    private void showImportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV file to import");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToImport = fileChooser.getSelectedFile();
            String filePath = fileToImport.getAbsolutePath();
            System.out.println("User selected file for import: " + filePath);

            new Thread(() -> {
                String message;
                try {
                    int importedCount = transactionService.importTransactionsFromCsv(currentUser.getTransactionFilePath(), filePath);
                    message = "Successfully imported " + importedCount + " transaction records.";
                    System.out.println(message);

                    String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        loadCSVDataForCurrentUser("");
                        clearSearchFields();
                        JOptionPane.showMessageDialog(this, finalMessage, "Import Successful", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception ex) {
                    message = "Import failed!\n" + ex.getMessage();
                    System.err.println("CSV Import failed: " + ex.getMessage());
                    ex.printStackTrace();
                    String finalMessage1 = message;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, finalMessage1, "Import Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        } else {
            System.out.println("User cancelled file selection.");
        }
    }

    // Inside MenuUI class, showAddTransactionDialog method
    private void showAddTransactionDialog() {
        JDialog addDialog = new JDialog();
        addDialog.setTitle("Add Transaction"); // "Add Transaction"
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField transactionTimeField = new JTextField(15);
        JTextField transactionTypeField = new JTextField(15);
        JButton aiSuggestButton = new JButton("AI Category Suggestion"); // "AI Category Suggestion"

        JTextField counterpartyField = new JTextField(15);
        JTextField commodityField = new JTextField(15);
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"Income", "Expense"}); // "Income", "Expense"
        JTextField paymentAmountField = new JTextField(15);
        JTextField paymentMethodField = new JTextField(15);
        JTextField currentStatusField = new JTextField(15);
        JTextField orderNumberField = new JTextField(15);
        JTextField merchantNumberField = new JTextField(15);
        JTextField remarksField = new JTextField(15);


        gbc.gridx = 0; gbc.gridy = 0; dialogPanel.add(new JLabel("Transaction Time:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; dialogPanel.add(transactionTimeField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1; dialogPanel.add(new JLabel("Transaction Type:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; dialogPanel.add(transactionTypeField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0; dialogPanel.add(aiSuggestButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Counterparty:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; dialogPanel.add(counterpartyField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Commodity:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; dialogPanel.add(commodityField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; dialogPanel.add(new JLabel("In/Out:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2; dialogPanel.add(inOutComboBox, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Amount(CNY):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2; dialogPanel.add(paymentAmountField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Payment Method:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 2; dialogPanel.add(paymentMethodField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Current Status:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.gridwidth = 2; dialogPanel.add(currentStatusField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Order Number:"));
        gbc.gridx = 1; gbc.gridy = 8; gbc.gridwidth = 2; dialogPanel.add(orderNumberField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Merchant Number:"));
        gbc.gridx = 1; gbc.gridy = 9; gbc.gridwidth = 2; dialogPanel.add(merchantNumberField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 1; dialogPanel.add(new JLabel("Remarks:"));
        gbc.gridx = 1; gbc.gridy = 10; gbc.gridwidth = 2; dialogPanel.add(remarksField, gbc); gbc.gridwidth = 1;

        JDialog waitingDialog = new JDialog(addDialog, "Please wait", true); // "Please wait"
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel("Getting AI category suggestion...")); // "Getting AI category suggestion..."
        waitingDialog.setSize(250, 100);
        waitingDialog.setResizable(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("Confirm"); // "Confirm"
        JButton cancelButton = new JButton("Cancel");   // "Cancel"
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        dialogPanel.add(buttonPanel, gbc);

        addDialog.add(dialogPanel, BorderLayout.CENTER);

        aiSuggestButton.addActionListener(e -> {
            System.out.println("AI Suggest button clicked (EDT).");
            aiSuggestButton.setEnabled(false);
            Transaction tempTransaction = new Transaction(
                    emptyIfNull(transactionTimeField.getText().trim()),
                    emptyIfNull(transactionTypeField.getText().trim()),
                    emptyIfNull(counterpartyField.getText().trim()),
                    emptyIfNull(commodityField.getText().trim()),
                    (String) inOutComboBox.getSelectedItem(),
                    safeParseDouble(paymentAmountField.getText().trim()),
                    emptyIfNull(paymentMethodField.getText().trim()),
                    emptyIfNull(currentStatusField.getText().trim()),
                    emptyIfNull(orderNumberField.getText().trim()),
                    emptyIfNull(merchantNumberField.getText().trim()),
                    emptyIfNull(remarksField.getText().trim())
            );
            new Thread(() -> {
                System.out.println("Background thread started for AI classification...");
                String aiSuggestion = null;
                try {
                    aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);
                    System.out.println("AI Classification returned in background thread: " + aiSuggestion);
                } catch (Exception ex) {
                    System.err.println("Error in background AI thread: " + ex.getMessage());
                    ex.printStackTrace();
                    aiSuggestion = "Error: " + ex.getMessage();
                }
                String finalSuggestion = aiSuggestion;
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Updating UI on EDT from background thread.");
                    waitingDialog.setVisible(false);
                    if (finalSuggestion != null && !finalSuggestion.isEmpty() && !finalSuggestion.startsWith("Error:")) {
                        if (StandardCategories.ALL_KNOWN_TYPES.contains(finalSuggestion.trim())) {
                            transactionTypeField.setText(finalSuggestion.trim());
                        } else {
                            System.err.println("AI returned non-standard category despite prompt: " + finalSuggestion);
                            JOptionPane.showMessageDialog(addDialog, "AI returned an unexpected category format:\n" + finalSuggestion + "\nPlease enter manually.", "AI Result Anomaly", JOptionPane.WARNING_MESSAGE);
                            transactionTypeField.setText("");
                        }
                    } else if (finalSuggestion != null && finalSuggestion.startsWith("Error:")) {
                        JOptionPane.showMessageDialog(addDialog, "Failed to get AI category suggestion!\n" + finalSuggestion.substring(6), "AI Error", JOptionPane.ERROR_MESSAGE);
                        transactionTypeField.setText("");
                    }
                    else {
                        JOptionPane.showMessageDialog(addDialog, "AI could not provide a category suggestion.", "AI Tip", JOptionPane.INFORMATION_MESSAGE);
                        transactionTypeField.setText("");
                    }
                    aiSuggestButton.setEnabled(true);
                    System.out.println("UI update complete, buttons re-enabled.");
                });
            }).start();
            System.out.println("Showing waiting dialog (EDT block continues here).");
            waitingDialog.setLocationRelativeTo(addDialog);
            waitingDialog.setVisible(true);
            System.out.println("waiting dialog is now hidden (EDT unblocked).");
        });

        confirmButton.addActionListener(e -> {
            String transactionTime = emptyIfNull(transactionTimeField.getText().trim());
            String finalTransactionType = emptyIfNull(transactionTypeField.getText().trim());
            String counterparty = emptyIfNull(counterpartyField.getText().trim());
            String commodity = emptyIfNull(commodityField.getText().trim());
            String inOut = (String) inOutComboBox.getSelectedItem();
            String paymentAmountText = paymentAmountField.getText().trim();
            String paymentMethod = emptyIfNull(paymentMethodField.getText().trim());
            String currentStatus = emptyIfNull(currentStatusField.getText().trim());
            String orderNumber = emptyIfNull(orderNumberField.getText().trim());
            String merchantNumber = emptyIfNull(merchantNumberField.getText().trim());
            String remarks = emptyIfNull(remarksField.getText().trim());

            if (orderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "Order Number cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (finalTransactionType.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "Transaction Type cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
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
            Transaction newTransaction = new Transaction(
                    transactionTime, finalTransactionType, counterparty, commodity, inOut,
                    paymentAmount, paymentMethod, currentStatus, orderNumber, merchantNumber, remarks
            );
            try {
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

    public void editRow(int rowIndex) {
        System.out.println("Editing row: " + rowIndex + " for user " + currentUser.getUsername());
        JDialog editDialog = new JDialog();
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            Vector<String> rowData = new Vector<>();
            for (int i = 0; i <= 10; i++) {
                Object value = this.tableModel.getValueAt(rowIndex, i);
                rowData.add(value != null ? value.toString() : "");
            }
            System.out.println("Retrieved row data from table model for editing: " + rowData);
            String originalOrderNumber = rowData.get(8).trim();
            if (originalOrderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Cannot edit: Order Number is empty!", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to edit row " + rowIndex + " but order number is empty.");
                return;
            }
            editDialog.setTitle("Edit Transaction (Order No: " + originalOrderNumber + ")"); // "Edit Transaction (Order No: "
            editDialog.setModal(true);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            JTextField[] fields = new JTextField[11];
            String[] fieldNames = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks"};
            JButton aiSuggestButton = new JButton("AI Category Suggestion"); // "AI Category Suggestion"

            for (int i = 0; i < fieldNames.length; i++) {
                gbc.gridx = 0; gbc.gridy = i; gbc.gridwidth = 1; gbc.weightx = 0.0; dialogPanel.add(new JLabel(fieldNames[i] + ":"), gbc);
                fields[i] = new JTextField(rowData.get(i));
                if (i == 1) {
                    gbc.gridx = 1; gbc.gridy = i; gbc.weightx = 1.0; dialogPanel.add(fields[i], gbc);
                    gbc.gridx = 2; gbc.gridy = i; gbc.weightx = 0.0; dialogPanel.add(aiSuggestButton, gbc);
                    gbc.gridwidth = 1;
                } else {
                    gbc.gridx = 1; gbc.gridy = i; gbc.gridwidth = 2; gbc.weightx = 1.0; dialogPanel.add(fields[i], gbc);
                    gbc.gridwidth = 1;
                }
            }
            fields[8].setEditable(false);

            JDialog waitingDialog = new JDialog(editDialog, "Please wait", true); // "Please wait"
            waitingDialog.setLayout(new FlowLayout());
            waitingDialog.add(new JLabel("Getting AI category suggestion...")); // "Getting AI category suggestion..."
            waitingDialog.setSize(250, 100);
            waitingDialog.setResizable(false);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton confirmButton = new JButton("Confirm"); // "Confirm"
            JButton cancelButton = new JButton("Cancel");   // "Cancel"
            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);
            gbc.gridx = 0; gbc.gridy = fieldNames.length; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; dialogPanel.add(buttonPanel, gbc);
            editDialog.add(dialogPanel, BorderLayout.CENTER);

            aiSuggestButton.addActionListener(e -> {
                System.out.println("AI Suggest button clicked (EDT) in edit dialog.");
                aiSuggestButton.setEnabled(false);
                Transaction tempTransaction = new Transaction(
                        fields[0].getText().trim(), fields[1].getText().trim(), fields[2].getText().trim(),
                        fields[3].getText().trim(), fields[4].getText().trim(), safeParseDouble(fields[5].getText().trim()),
                        fields[6].getText().trim(), fields[7].getText().trim(), fields[8].getText().trim(),
                        fields[9].getText().trim(), fields[10].getText().trim()
                );
                new Thread(() -> {
                    System.out.println("Background thread started for AI classification (edit dialog)...");
                    String aiSuggestion = null;
                    try {
                        aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);
                        System.out.println("AI Classification returned in background thread (edit dialog): " + aiSuggestion);
                    } catch (Exception ex) {
                        System.err.println("Error in background AI thread (edit dialog): " + ex.getMessage());
                        ex.printStackTrace();
                        aiSuggestion = "Error: " + ex.getMessage();
                    }
                    String finalSuggestion = aiSuggestion;
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("Updating UI on EDT from background thread (edit dialog).");
                        waitingDialog.setVisible(false);
                        if (finalSuggestion != null && !finalSuggestion.isEmpty() && !finalSuggestion.startsWith("Error:")) {
                            if (StandardCategories.ALL_KNOWN_TYPES.contains(finalSuggestion.trim())) {
                                fields[1].setText(finalSuggestion.trim());
                            } else {
                                System.err.println("AI returned non-standard category despite prompt: " + finalSuggestion);
                                JOptionPane.showMessageDialog(editDialog, "AI returned an unexpected category format:\n" + finalSuggestion + "\nPlease enter manually.", "AI Result Anomaly", JOptionPane.WARNING_MESSAGE);
                                fields[1].setText("");
                            }
                        } else if (finalSuggestion != null && finalSuggestion.startsWith("Error:")) {
                            JOptionPane.showMessageDialog(editDialog, "Failed to get AI category suggestion!\n" + finalSuggestion.substring(6), "AI Error", JOptionPane.ERROR_MESSAGE);
                            fields[1].setText("");
                        }
                        else {
                            JOptionPane.showMessageDialog(editDialog, "AI could not provide a category suggestion.", "AI Tip", JOptionPane.INFORMATION_MESSAGE);
                            fields[1].setText("");
                        }
                        aiSuggestButton.setEnabled(true);
                        System.out.println("UI update complete, buttons re-enabled (edit dialog).");
                    });
                }).start();
                System.out.println("Showing waiting dialog (EDT block continues here in edit dialog).");
                waitingDialog.setLocationRelativeTo(editDialog);
                waitingDialog.setVisible(true);
                System.out.println("waiting dialog is now hidden (EDT unblocked in edit dialog).");
            });

            confirmButton.addActionListener(e -> {
                String transactionTime = fields[0].getText().trim();
                String finalTransactionType = fields[1].getText().trim();
                String counterparty = fields[2].getText().trim();
                String commodity = fields[3].getText().trim();
                String inOut = fields[4].getText().trim();
                String paymentAmountText = fields[5].getText().trim();
                String paymentMethod = fields[6].getText().trim();
                String currentStatus = fields[7].getText().trim();
                String merchantNumber = fields[9].getText().trim();
                String remarks = fields[10].getText().trim();

                if (finalTransactionType.isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog, "Transaction Type cannot be empty!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!StandardCategories.ALL_KNOWN_TYPES.contains(finalTransactionType)) {
                    JOptionPane.showMessageDialog(editDialog, "Transaction type must be one of the standard categories!\nAllowed categories:\n" + StandardCategories.getAllCategoriesString(), "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!inOut.equalsIgnoreCase("Income") && !inOut.equalsIgnoreCase("Expense") &&
                        !inOut.equalsIgnoreCase("In") && !inOut.equalsIgnoreCase("Out")) {
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
                Transaction updatedTransaction = new Transaction(
                        transactionTime, finalTransactionType, counterparty, commodity, inOut,
                        paymentAmount, paymentMethod, currentStatus, originalOrderNumber,
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
            editDialog.pack();
            editDialog.setLocationRelativeTo(this);
            editDialog.setVisible(true);
        } else {
            System.err.println("Attempted to edit row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel generalAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField userRequestField = new JTextField(40);
        aiStartTimeField = new JTextField(10);
        aiEndTimeField = new JTextField(10);
        aiAnalyzeButton = new JButton("General Analysis (Raw Data)"); // "General Analysis (Raw Data)"

        generalAnalysisPanel.add(new JLabel("General Analysis Request:")); // "General Analysis Request:"
        generalAnalysisPanel.add(userRequestField);
        generalAnalysisPanel.add(new JLabel("Time Range (yyyy/MM/dd HH:mm): From:")); // "Time Range (yyyy/MM/dd HH:mm): From:"
        generalAnalysisPanel.add(aiStartTimeField);
        generalAnalysisPanel.add(new JLabel("To:")); // "To:"
        generalAnalysisPanel.add(aiAnalyzeButton);

        JPanel summaryAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiPersonalSummaryButton = new JButton("Personal Spending Summary"); // "Personal Spending Summary"
        aiSavingsGoalsButton = new JButton("Savings Goal Suggestions");   // "Savings Goal Suggestions"
        aiPersonalSavingTipsButton = new JButton("Personalized Saving Tips"); // "Personalized Saving Tips"

        summaryAnalysisPanel.add(new JLabel("Based on Monthly Summary Analysis:")); // "Based on Monthly Summary Analysis:"
        summaryAnalysisPanel.add(aiPersonalSummaryButton);
        summaryAnalysisPanel.add(aiSavingsGoalsButton);
        summaryAnalysisPanel.add(aiPersonalSavingTipsButton);

        JPanel csButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiBudgetButton = new JButton("Budget Suggestion (Student)"); // "Budget Suggestion (Student)"
        aiTipsButton = new JButton("Saving Tips (Student)");       // "Saving Tips (Student)"
        csButtonsPanel.add(new JLabel("Student-Specific Features:")); // "Student-Specific Features:"
        csButtonsPanel.add(aiBudgetButton);
        csButtonsPanel.add(aiTipsButton);

        JPanel topControlPanel = new JPanel();
        topControlPanel.setLayout(new BoxLayout(topControlPanel, BoxLayout.Y_AXIS));
        topControlPanel.add(generalAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(summaryAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(csButtonsPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(topControlPanel, BorderLayout.NORTH);
        aiResultArea = new JTextArea();
        aiResultArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // "Microsoft YaHei" is a common font name for Chinese
        aiResultArea.setLineWrap(true);
        aiResultArea.setWrapStyleWord(true);
        aiResultArea.setEditable(false);
        aiResultArea.setText("Welcome to the AI Personal Finance Analysis feature.\n\n" +
                "You can try the following operations:\n" +
                "1. Enter a general analysis request in the input field above (based on raw data, time range can be specified), then click \"General Analysis\".\n" +
                "2. Click \"Personal Spending Summary\" to get a detailed summary based on your monthly income and expenses.\n" +
                "3. Click \"Savings Goal Suggestions\" to get savings advice based on your income and expenditure situation.\n" +
                "4. Click \"Personalized Saving Tips\" to get saving advice based on your spending categories.\n" +
                "5. Student users can click \"Budget Suggestion\" and \"Saving Tips\" for exclusive advice.\n");

        JScrollPane resultScrollPane = new JScrollPane(aiResultArea);
        panel.add(resultScrollPane, BorderLayout.CENTER);

        aiPersonalSummaryButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personal Spending Summary ---\n\nGenerating summary based on your monthly spending data, please wait...\n");
            setAIButtonsEnabled(false);
            new Thread(() -> {
                String result = aiTransactionService.generatePersonalSummary(currentUser.getTransactionFilePath());
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Personal Spending Summary ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });

        aiSavingsGoalsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Savings Goal Suggestions ---\n\nGenerating savings goal suggestions based on your income and expenses, please wait...\n");
            setAIButtonsEnabled(false);
            new Thread(() -> {
                String result = aiTransactionService.suggestSavingsGoals(currentUser.getTransactionFilePath());
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Savings Goal Suggestions ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });

        aiPersonalSavingTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personalized Saving Tips ---\n\nGenerating saving tips based on your spending categories, please wait...\n");
            setAIButtonsEnabled(false);
            new Thread(() -> {
                String result = aiTransactionService.givePersonalSavingTips(currentUser.getTransactionFilePath());
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Personalized Saving Tips ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });

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
            new Thread(() -> {
                String result = aiTransactionService.analyzeTransactions(userRequest, currentUser.getTransactionFilePath(), startTimeStr, endTimeStr);
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- General Analysis Result ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });

        aiBudgetButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Budget Suggestion ---\n\nGenerating budget suggestion based on your historical spending, please wait...\n");
            setAIButtonsEnabled(false);
            new Thread(() -> {
                String resultMessage;
                try {
                    double[] budgetRange = collegeStudentNeeds.generateBudget(currentUser.getTransactionFilePath());
                    if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] != -1) {
                        resultMessage = String.format("Based on your spending records, the recommended budget range for next week is: [%.2f CNY, %.2f CNY]", budgetRange[0], budgetRange[1]);
                    } else if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] == -1) {
                        resultMessage = "Not enough spending records to calculate weekly budget suggestions.";
                    }
                    else {
                        resultMessage = "Failed to generate budget suggestion, AI did not return a valid range.";
                        System.err.println("AI Budget generation failed, invalid response format.");
                    }
                } catch (Exception ex) {
                    resultMessage = "Failed to generate budget suggestion!\n" + ex.getMessage();
                    System.err.println("Error generating AI budget:");
                    ex.printStackTrace();
                }
                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Student Budget Suggestion ---\n\n" + finalResultMessage);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });

        aiTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Saving Tips ---\n\nGenerating saving tips, please wait...\n");
            setAIButtonsEnabled(false);
            new Thread(() -> {
                String resultMessage;
                try {
                    resultMessage = collegeStudentNeeds.generateTipsForSaving(currentUser.getTransactionFilePath());
                } catch (Exception ex) {
                    resultMessage = "Failed to generate saving tips!\n" + ex.getMessage();
                    System.err.println("Error generating AI tips:");
                    ex.printStackTrace();
                }
                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Student Saving Tips ---\n\n" + finalResultMessage);
                    setAIButtonsEnabled(true);
                });
            }).start();
        });
        return panel;
    }

    private void setAIButtonsEnabled(boolean enabled) {
        if (aiAnalyzeButton != null) aiAnalyzeButton.setEnabled(enabled);
        if (aiBudgetButton != null) aiBudgetButton.setEnabled(enabled);
        if (aiTipsButton != null) aiTipsButton.setEnabled(enabled);
        if (aiPersonalSummaryButton != null) aiPersonalSummaryButton.setEnabled(enabled);
        if (aiSavingsGoalsButton != null) aiSavingsGoalsButton.setEnabled(enabled);
        if (aiPersonalSavingTipsButton != null) aiPersonalSavingTipsButton.setEnabled(enabled);
    }

    private JPanel createAdminStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateStatsButton = new JButton("Generate/Update Statistics"); // "Generate/Update Statistics"
        refreshDisplayButton = new JButton("Refresh Display");        // "Refresh Display"
        controlPanel.add(generateStatsButton);
        controlPanel.add(refreshDisplayButton);
        panel.add(controlPanel, BorderLayout.NORTH);
        adminStatsArea = new JTextArea();
        adminStatsArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // "Microsoft YaHei"
        adminStatsArea.setEditable(false);
        adminStatsArea.setLineWrap(true);
        adminStatsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(adminStatsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        generateStatsButton.addActionListener(e -> {
            adminStatsArea.setText("Generating/Updating summary statistics, please wait...\n"); // "Generating/Updating summary statistics, please wait...\n"
            generateStatsButton.setEnabled(false);
            refreshDisplayButton.setEnabled(false);
            new Thread(() -> {
                String message;
                try {
                    summaryStatisticService.generateAndSaveWeeklyStatistics();
                    message = "Summary statistics generated/updated successfully!\nPlease click 'Refresh Display' to view the latest data."; // "Summary statistics generated/updated successfully!\nPlease click 'Refresh Display' to view the latest data."
                    System.out.println(message);
                } catch (Exception ex) {
                    message = "Failed to generate/update summary statistics!\n" + ex.getMessage(); // "Failed to generate/update summary statistics!\n"
                    System.err.println(message);
                    ex.printStackTrace();
                }
                String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    adminStatsArea.setText(finalMessage);
                    generateStatsButton.setEnabled(true);
                    refreshDisplayButton.setEnabled(true);
                });
            }).start();
        });

        refreshDisplayButton.addActionListener(e -> {
            displaySummaryStatistics();
        });

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> adminStatsArea.setText("Loading existing statistics...\n")); // "Loading existing statistics...\n"
            try {
                List<SummaryStatistic> initialStats = summaryStatisticService.getAllSummaryStatistics();
                if (!initialStats.isEmpty()) {
                    SwingUtilities.invokeLater(this::displaySummaryStatistics);
                } else {
                    SwingUtilities.invokeLater(() -> adminStatsArea.setText("No existing summary statistics found.\nPlease click the 'Generate/Update Statistics' button to generate them.")); // "No existing summary statistics found.\nPlease click the 'Generate/Update Statistics' button to generate them."
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> adminStatsArea.setText("Failed to load existing statistics!\n" + ex.getMessage())); // "Failed to load existing statistics!\n"
                ex.printStackTrace();
            }
        }).start();
        return panel;
    }

    private void displaySummaryStatistics() {
        adminStatsArea.setText("Loading summary statistics...\n"); // "Loading summary statistics...\n"
        if(generateStatsButton != null) generateStatsButton.setEnabled(false);
        if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(false);

        new Thread(() -> {
            String displayContent;
            try {
                List<SummaryStatistic> stats = summaryStatisticService.getAllSummaryStatistics();
                if (stats.isEmpty()) {
                    displayContent = "No summary statistics currently available.\nPlease click the 'Generate/Update Statistics' button first."; // "No summary statistics currently available.\nPlease click the 'Generate/Update Statistics' button first."
                } else {
                    StringBuilder sb = new StringBuilder("===== Summary Statistics =====\n\n"); // "===== Summary Statistics =====\n\n"
                    stats.sort(Comparator.comparing(SummaryStatistic::getWeekIdentifier));
                    for (int i = stats.size() - 1; i >= 0; i--) {
                        SummaryStatistic stat = stats.get(i);
                        sb.append("Week Identifier: ").append(stat.getWeekIdentifier()).append("\n"); // "Week Identifier: "
                        sb.append("  Total Income: ").append(String.format("%.2f", stat.getTotalIncomeAllUsers())).append(" CNY\n"); // "  Total Income: " ... " CNY\n"
                        sb.append("  Total Expense: ").append(String.format("%.2f", stat.getTotalExpenseAllUsers())).append(" CNY\n"); // "  Total Expense: " ... " CNY\n"
                        if (stat.getTopExpenseCategoryAmount() > 0) {
                            sb.append("  Top Expense Category: ").append(stat.getTopExpenseCategory()).append(" (").append(String.format("%.2f", stat.getTopExpenseCategoryAmount())).append(" CNY)\n"); // "  Top Expense Category: " ... " CNY)\n"
                        } else {
                            sb.append("  Top Expense Category: No significant expense category\n"); // "  Top Expense Category: No significant expense category\n"
                        }
                        sb.append("  Number of Participating Users: ").append(stat.getNumberOfUsersWithTransactions()).append("\n"); // "  Number of Participating Users: "
                        sb.append("  Generated Time: ").append(stat.getTimestampGenerated()).append("\n"); // "  Generated Time: "
                        sb.append("--------------------\n");
                    }
                    displayContent = sb.toString();
                }
            } catch (IOException ex) {
                displayContent = "Failed to load summary statistics!\n" + ex.getMessage(); // "Failed to load summary statistics!\n"
                ex.printStackTrace();
            }
            String finalDisplayContent = displayContent;
            SwingUtilities.invokeLater(() -> {
                adminStatsArea.setText(finalDisplayContent);
                if(generateStatsButton != null) generateStatsButton.setEnabled(true);
                if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(true);
            });
        }).start();
    }

    public void deleteRow(int rowIndex) {
        System.out.println("Attempting to delete row: " + rowIndex + " for user " + currentUser.getUsername());
        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            String orderNumber = (String) this.tableModel.getValueAt(rowIndex, 8);
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Cannot delete: Order Number is empty!", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to delete row " + rowIndex + " but order number is null or empty.");
                return;
            }
            orderNumber = orderNumber.trim();
            System.out.println("Deleting transaction with order number: " + orderNumber);
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete the transaction with order number '" + orderNumber + "'?", // "Are you sure you want to delete the transaction with order number '" ... "'?"
                    "Confirm Delete", // "Confirm Delete"
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    boolean deleted = transactionService.deleteTransaction(orderNumber);
                    if (deleted) {
                        this.tableModel.removeRow(rowIndex);
                        JOptionPane.showMessageDialog(null, "Delete successful!", "Information", JOptionPane.INFORMATION_MESSAGE); // "Delete successful!"
                        System.out.println("Deleted row " + rowIndex + " with order number " + orderNumber + " from UI.");
                        triggerCurrentSearch();
                    } else {
                        JOptionPane.showMessageDialog(null, "Delete failed: Corresponding order number " + orderNumber + " not found", "Error", JOptionPane.ERROR_MESSAGE); // "Delete failed: Corresponding order number " ... " not found"
                        System.err.println("Delete failed: order number " + orderNumber + " not found by service.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Delete failed!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // "Delete failed!\n"
                    System.err.println("Error during deletion for order number " + orderNumber);
                }
            } else {
                System.out.println("Deletion cancelled by user for order number: " + orderNumber);
            }
        } else {
            System.err.println("Attempted to delete row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    private Vector<String> createRowFromTransaction(Transaction transaction) {
        Vector<String> row = new Vector<>();
        row.add(emptyIfNull(transaction.getTransactionTime()));
        row.add(emptyIfNull(transaction.getTransactionType()));
        row.add(emptyIfNull(transaction.getCounterparty()));
        row.add(emptyIfNull(transaction.getCommodity()));
        row.add(emptyIfNull(transaction.getInOut()));
        row.add(String.valueOf(transaction.getPaymentAmount()));
        row.add(emptyIfNull(transaction.getPaymentMethod()));
        row.add(emptyIfNull(transaction.getCurrentStatus()));
        row.add(emptyIfNull(transaction.getOrderNumber()));
        row.add(emptyIfNull(transaction.getMerchantNumber()));
        row.add(emptyIfNull(transaction.getRemarks()));
        row.add("Modify");
        row.add("Delete");
        return row;
    }

    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) {
        System.out.println("Searching with criteria: time='" + query1 + "', type='" + query2 + "', counterparty='" + query3 + "', commodity='" + query4 + "', inOut='" + query6 + "', paymentMethod='" + query5 + "'");
        this.tableModel.setRowCount(0);
        Transaction searchCriteria = new Transaction(
                query1, query2, query3, query4, query6,
                0,
                query5,
                "", "", "", ""
        );
        try {
            List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);
            System.out.println("Search results count: " + transactions.size());
            for (Transaction transaction : transactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                this.tableModel.addRow(row);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Search failed!\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // "Search failed!\n"
        }
    }

    private double safeParseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { System.err.println("Failed to parse double from string: '" + value + "'"); return 0.0; }
    }
    private void clearSearchFields() {
        searchTransactionTimeField.setText("");
        searchTransactionTypeField.setText("");
        searchCounterpartyField.setText("");
        searchCommodityField.setText("");
        searchInOutComboBox.setSelectedItem("");
        searchPaymentMethodField.setText("");
        System.out.println("Cleared search fields.");
    }
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
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
    public JTable getTable() {
        return table;
    }
}