package Controller;

import Constants.StandardCategories; // Import StandardCategories if needed in UI
import Service.AIservice.AITransactionService; // Import AI services
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.SummaryStatisticService; // Import SummaryStatisticService
import Service.TransactionService;
import Service.User.UserService;
import model.SummaryStatistic; // Import SummaryStatistic
import model.Transaction;
import model.User;
// import Constants.StandardCategories; // Already imported above

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter; // Added for Export
import java.io.IOException;
import java.nio.file.Files; // Added for Export
import java.nio.file.Paths; // Added for Export
import java.util.ArrayList; // Added for Export
import java.util.List;
import java.util.Vector;
import java.util.Comparator; // For sorting stats display
import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.stream.Collectors; // Added for loadCSVDataForCurrentUser
import java.awt.Dimension;

// Added for Export (Apache Commons CSV)
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

// Added for Batch AI (pre-existing in pre)
import java.util.concurrent.atomic.AtomicInteger;


public class MenuUI extends JPanel { // Extend JPanel for easier use in Main (optional but common)

    private final User currentUser;
    private final TransactionService transactionService;
    private final SummaryStatisticService summaryStatisticService;
    private final AITransactionService aiTransactionService;
    private final CollegeStudentNeeds collegeStudentNeeds;
    private final ExecutorService executorService;
    private final UserService userService; // Added UserService for user management

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
    // REMOVED in post: private HistogramPanelContainer histogramPanelContainer; // No longer needed

    private JPanel rightPanel;
    private CardLayout cardLayout;

    // UI components for AI panel (existing + new)
    private JTextArea aiResultArea;
    private JTextField aiStartTimeField;
    private JTextField aiEndTimeField;
    private JButton aiAnalyzeButton;
    private JButton aiBudgetButton;
    private JButton aiTipsButton;
    private JButton aiPersonalSummaryButton;
    private JButton aiSavingsGoalsButton;
    private JButton aiPersonalSavingTipsButton;
    private JButton runBatchAiButton; // Existing in pre
    private JButton aiSeasonalAnalysisButton; // NEW: Added seasonal analysis button from post


    // UI components for Admin Stats panel (existing)
    private JTextArea adminStatsArea;
    private JButton generateStatsButton;
    private JButton refreshDisplayButton;

    // Panel for Visualization (existing in pre)
    private VisualizationPanel visualizationPanel; // Add instance field

    // New panel for User Management (Admin) (existing in pre)
    private UserManagerPanel userManagerPanel; // Add instance field

    /**
     * Constructor to initialize the main UI panel and its components.
     * This constructor is from MenuUI-pre.txt and accepts all necessary services.
     *
     * @param authenticatedUser The currently logged-in user.
     * @param transactionService User-specific transaction service.
     * @param summaryStatisticService Summary statistics service (for admin).
     * @param aiTransactionService AI transaction service.
     * @param collegeStudentNeeds College student specific AI service.
     * @param executorService Executor service for background tasks.
     * @param userService UserService for user management (for admin).
     */
    public MenuUI(User authenticatedUser, TransactionService transactionService,
                  SummaryStatisticService summaryStatisticService,
                  AITransactionService aiTransactionService,
                  CollegeStudentNeeds collegeStudentNeeds,
                  ExecutorService executorService,
                  UserService userService) { // Accept ExecutorService

        this.currentUser = authenticatedUser;
        this.transactionService = transactionService;
        this.summaryStatisticService = summaryStatisticService;
        this.aiTransactionService = aiTransactionService;
        this.collegeStudentNeeds = collegeStudentNeeds;
        this.executorService = executorService;
        this.userService = userService; // Assign UserServic

        // Initialize table model (same as before)
        String[] columnNames = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks", "Modify", "Delete"};
        this.tableModel = new DefaultTableModel(columnNames, 0);
        this.table = new JTable(this.tableModel);

        // Set the layout manager for this JPanel (MenuUI) (same as before)
        setLayout(new BorderLayout());

        // Add the left navigation panel (modified to include User Management button)
        add(createLeftPanel(), BorderLayout.WEST);

        // Add the right content panel (uses CardLayout) (modified to include User Management panel)
        setupRightPanel();
        add(rightPanel, BorderLayout.CENTER);

        // Initial data load is done in createMainPanel (same as before)
        // loadCSVDataForCurrentUser("Income");

        System.out.println("MenuUI initialized for user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
    }

    // REMOVED: The second confusing constructor from pre.txt


    /**
     * Creates and returns the main JPanel for the MenuUI.
     * This method is typically called once from Main.java after the MenuUI instance is created.
     * (Same as before)
     *
     * @return The main JPanel (which is the MenuUI instance itself).
     */
    public JPanel createMainPanel() {
        // The MenuUI JPanel itself is the main panel.
        // Load initial data here now that the UI components are set up.
        loadCSVDataForCurrentUser(""); // Load all data initially

        return this; // Return this MenuUI instance
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
            JOptionPane.showMessageDialog(this, "Failed to load user transaction data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to create the left panel (Menu/AI/Admin/Visualization/User Management buttons) - MODIFIED (from pre)
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton menuButton = new JButton("Transaction List"); // "交易列表"
        JButton aiButton = new JButton("AI Analysis");       // "AI分析"
        JButton adminStatsButton = new JButton("Admin Stats");     // "管理员统计"
        JButton visualizationButton = new JButton("Visualization"); // "可视化"
        JButton userManagerButton = new JButton("User Management"); // NEW: User Management button from pre


        // Set consistent size for buttons
        Dimension buttonSize = new Dimension(150, 40);
        menuButton.setMaximumSize(buttonSize);
        aiButton.setMaximumSize(buttonSize);
        adminStatsButton.setMaximumSize(buttonSize);
        visualizationButton.setMaximumSize(buttonSize);
        userManagerButton.setMaximumSize(buttonSize); // Set size for new button


        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        aiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminStatsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        visualizationButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        userManagerButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Align new button


        leftPanel.add(menuButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(aiButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add Admin-only buttons (from pre)
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            leftPanel.add(adminStatsButton); // Admin Stats button
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            leftPanel.add(userManagerButton); // NEW: User Management button
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            System.out.println("Admin user logged in, showing Admin and User Management buttons.");
        } else {
            System.out.println("Regular user logged in, hiding Admin buttons.");
        }


        // Add Visualization button (visible for all users) (from pre)
        leftPanel.add(visualizationButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));


        // Add action listeners (existing for Menu, AI, Admin, Visualization, and User Management) (from pre)
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table");
            loadCSVDataForCurrentUser("Income"); // Load "Income" by default or "" for all
        });

        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "AI");
        });

        visualizationButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Visualization"); // Switch to visualization view
            if (visualizationPanel != null) { // Ensure panel is initialized
                visualizationPanel.refreshPanelData(); // Call refresh method
            }
        });


        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            adminStatsButton.addActionListener(e -> {
                cardLayout.show(rightPanel, "AdminStats");
                displaySummaryStatistics(); // Refresh stats display when switching
            });

            // NEW: Add action listener for User Management button (from pre)
            userManagerButton.addActionListener(e -> {
                cardLayout.show(rightPanel, "UserManagement"); // Switch to User Management view
                // Optional: Trigger initial data load or setup in UserManagerPanel
                if (userManagerPanel != null) {
                    userManagerPanel.refreshPanelData(); // Call refresh method
                }
            });
        }


        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    // Method to set up the right panel, adding different views - MODIFIED (from pre)
    private void setupRightPanel() {
        this.cardLayout = new CardLayout();
        this.rightPanel = new JPanel(this.cardLayout);

        // Create and add different panels (views)
        JPanel tablePanel = createTablePanel(); // Table view (from pre)
        JPanel aiPanel = createAIPanel(); // AI view (modified in this merge)
        JPanel adminStatsPanel = createAdminStatsPanel(); // Admin stats view (from pre)
        this.visualizationPanel = new VisualizationPanel(this.transactionService); // Visualization panel (from pre)

        // NEW: Create User Management Panel and inject UserService and ExecutorService (from pre)
        this.userManagerPanel = new UserManagerPanel(this.userService, this.executorService);


        rightPanel.add(tablePanel, "Table");
        rightPanel.add(aiPanel, "AI");
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            rightPanel.add(adminStatsPanel, "AdminStats");
            rightPanel.add(userManagerPanel, "UserManagement"); // NEW: Add User Management card
        }
        rightPanel.add(visualizationPanel, "Visualization");


        // Set the initially visible card (Table view)
        cardLayout.show(rightPanel, "Table");
    }

    // Method to create the table panel - same as before (from pre)
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

    // Inside MenuUI class, createInputPanel method - MODIFIED (from pre, includes Export)
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
        JButton exportButton = new JButton("Export CSV"); // NEW: Export button from pre


        inputPanel.add(searchButton);
        inputPanel.add(addButton);
        inputPanel.add(importButton);
        inputPanel.add(exportButton); // Add export button from pre


        searchButton.addActionListener(e -> triggerCurrentSearch());
        addButton.addActionListener(e -> showAddTransactionDialog());

        importButton.addActionListener(e -> {
            showImportDialog();
        });

        // Add ActionListener for Export button - NEW (from pre)
        exportButton.addActionListener(e -> {
            showExportDialog(); // Call a new method to handle export
        });

        return inputPanel;
    }

    // Inside MenuUI class, showImportDialog method - (from pre, uses ExecutorService)
    private void showImportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV file to import");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToImport = fileChooser.getSelectedFile();
            String filePath = fileToImport.getAbsolutePath();
            System.out.println("User selected file for import: " + filePath);

            // Submit import task to the ExecutorService (from pre)
            executorService.submit(() -> { // Use submit
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

    // Inside MenuUI class, showExportDialog method - NEW (from pre, uses ExecutorService)
    private void showExportDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV file"); // "Save CSV file"
        // Set a default file name
        fileChooser.setSelectedFile(new java.io.File("transactions_export.csv"));
        // Add file filter for .csv files
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        // Show save dialog
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();

            // Ensure file extension is .csv if not already provided
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
                fileToSave = new java.io.File(filePath); // Update File object
            }

            System.out.println("User selected file for export: " + filePath);

            // Execute export logic in a background thread to avoid blocking UI
            String finalFilePath = filePath; // Final variable for lambda
            executorService.submit(() -> { // Use submit (from pre)
                System.out.println("Export task submitted to ExecutorService for file: " + finalFilePath);
                String message;
                try {
                    // Get data from the current table model
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    int rowCount = model.getRowCount();
                    int colCount = model.getColumnCount(); // Includes Modify/Delete columns

                    // Get headers from table model
                    String[] headers = new String[colCount - 2]; // Exclude Modify and Delete columns
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = model.getColumnName(i);
                    }

                    // Use Apache Commons CSV to write to file
                    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(finalFilePath), java.nio.charset.StandardCharsets.UTF_8);
                         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers).withTrim())) {

                        // Write data rows
                        for (int i = 0; i < rowCount; i++) {
                            // Create a list/array for the row data, excluding the last two columns
                            List<String> rowData = new ArrayList<>();
                            for (int j = 0; j < colCount - 2; j++) { // Exclude Modify/Delete columns
                                Object cellValue = model.getValueAt(i, j);
                                rowData.add(cellValue != null ? cellValue.toString() : "");
                            }
                            csvPrinter.printRecord(rowData); // Write the row
                        }
                        csvPrinter.flush(); // Ensure data is written

                        message = "Successfully exported " + rowCount + " transaction records to:\n" + finalFilePath; // "Successfully exported " ... " transaction records to:\n"
                        System.out.println("Export task finished: " + message);

                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> { // Update UI on EDT
                            JOptionPane.showMessageDialog(this, finalMessage, "Export Successful", JOptionPane.INFORMATION_MESSAGE); // "Export Successful"
                        });

                    } catch (IOException ex) {
                        message = "Export failed!\n" + ex.getMessage();
                        System.err.println("Export task failed: " + ex.getMessage());
                        ex.printStackTrace();
                        String finalMessage1 = message;
                        SwingUtilities.invokeLater(() -> { // Update UI on EDT
                            JOptionPane.showMessageDialog(this, finalMessage1, "Export Error", JOptionPane.ERROR_MESSAGE); // "Export Error"
                        });
                    }

                } catch (Exception e) {
                    // This catch block is for errors within the executorService task setup, not the IOException itself.
                    System.err.println("Error during export task setup: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        JOptionPane.showMessageDialog(this, "An unexpected error occurred during export: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE); // "Export Error"
                    });
                }
            });

        } else {
            System.out.println("User cancelled file saving."); // "User cancelled file saving."
        }
    }


    // Inside MenuUI class, showAddTransactionDialog method - (from pre, uses ExecutorService for AI)
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


        // Add components using GridBagLayout (from pre layout structure)
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

        int row = 0; // Use a row counter
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
                dialogPanel.add(textFields[textFieldIndex++], gbc); // Add Transaction Type field

                // Constraints for AI Suggest Button (Column 2)
                gbc.gridx = 2; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0;
                gbc.fill = GridBagConstraints.NONE; // Button doesn't fill space
                dialogPanel.add(aiSuggestButton, gbc);
                gbc.fill = GridBagConstraints.HORIZONTAL; // Reset fill

            } else if (fieldName.equals("In/Out")) { // Row for In/Out ComboBox
                gbc.gridwidth = 2; // ComboBox spans 2 columns
                dialogPanel.add(inOutComboBox, gbc);

            } else { // All other TextFields
                gbc.gridwidth = 2; // These fields span 2 columns (1 and 2)
                dialogPanel.add(textFields[textFieldIndex++], gbc); // Add the text field
            }

            row++; // Move to the next row
        }

        // Order Number field should be editable for adding (from pre)
        orderNumberField.setEditable(true);


        JDialog waitingDialog = new JDialog(addDialog, "Please wait", true); // "Please wait"
        waitingDialog.setLayout(new FlowLayout());
        waitingDialog.add(new JLabel("Getting AI category suggestion...")); // "Getting AI category suggestion..."
        waitingDialog.setSize(250, 100);
        waitingDialog.setResizable(false);
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Prevent closing with X (from pre)


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("Confirm"); // "Confirm"
        JButton cancelButton = new JButton("Cancel");   // "Cancel"
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // Constraints for Button Panel (placed below the last field row) (from pre layout structure)
        gbc.gridx = 0; gbc.gridy = fieldNames.length; // Row after the last field
        gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(15, 5, 5, 5); // Add some space above buttons
        gbc.weightx = 0.0; gbc.weighty = 1.0; // Give this row vertical weight to push fields up
        dialogPanel.add(buttonPanel, gbc);

        addDialog.add(dialogPanel, BorderLayout.CENTER);


        // Add AI Suggest button action listener - Uses ExecutorService (from pre)
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
            executorService.submit(() -> { // Use submit (from pre)
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
                    if (paymentAmount < 0) { // Non-negative check from pre
                        JOptionPane.showMessageDialog(addDialog, "Amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Validate non-negative amount consistency with In/Out (from pre)
                    if (paymentAmount < 0 && (inOut != null && inOut.equals("Income"))) { // Assuming "Income" or "Expense" from ComboBox
                        JOptionPane.showMessageDialog(addDialog, "Income amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (paymentAmount < 0 && (inOut != null && inOut.equals("Expense"))) {
                        JOptionPane.showMessageDialog(addDialog, "Expense amount cannot be negative!", "Input Error", JOptionPane.ERROR_MESSAGE);
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

    // Inside MenuUI class, editRow method - (from pre, uses ExecutorService for AI)
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

            // Fields for the dialog (from pre structure)
            JTextField transactionTimeField = new JTextField(rowData.get(0));
            JTextField transactionTypeField = new JTextField(rowData.get(1));
            JButton aiSuggestButton = new JButton("AI Category Suggestion"); // "AI Category Suggestion"
            JTextField counterpartyField = new JTextField(rowData.get(2));
            JTextField commodityField = new JTextField(rowData.get(3));
            JComboBox<String> editInOutComboBox = new JComboBox<>(new String[]{"Income", "Expense"}); // JComboBox for In/Out (from pre)
            // Set initial value for In/Out ComboBox (from pre)
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

            // Disable Order Number field editing (from pre)
            orderNumberField.setEditable(false);

            // Add components using GridBagLayout (from pre layout structure)
            String[] fieldNames = {
                    "Transaction Time", "Transaction Type", "Counterparty", "Commodity",
                    "In/Out", "Amount(CNY)", "Payment Method", "Current Status",
                    "Order Number", "Merchant Number", "Remarks"
            };
            // Map field names to the created components for easier iteration (from pre structure)
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

            int row = 0; // Use a row counter
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

            JDialog waitingDialog = new JDialog(editDialog, "Please wait", true); // "Please wait"
            waitingDialog.setLayout(new FlowLayout());
            waitingDialog.add(new JLabel("Getting AI category suggestion...")); // "Getting AI category suggestion..."
            waitingDialog.setSize(250, 100);
            waitingDialog.setResizable(false);
            waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Prevent closing with X (from pre)


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton confirmButton = new JButton("Confirm"); // "Confirm"
            JButton cancelButton = new JButton("Cancel");   // "Cancel"
            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);

            // Constraints for Button Panel (from pre layout structure)
            gbc.gridx = 0; gbc.gridy = fieldNames.length; // Row after the last field
            gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(15, 5, 5, 5); // Add some space above buttons
            gbc.weightx = 0.0; gbc.weighty = 1.0; // Give this row vertical weight
            dialogPanel.add(buttonPanel, gbc);

            editDialog.add(dialogPanel, BorderLayout.CENTER);

            // Add AI Suggest button action listener - Uses ExecutorService (from pre)
            aiSuggestButton.addActionListener(e -> {
                System.out.println("AI Suggest button clicked (EDT) in edit dialog.");
                aiSuggestButton.setEnabled(false);

                // Build temporary transaction object from dialog components' current values (from pre)
                Transaction tempTransaction = new Transaction(
                        transactionTimeField.getText().trim(),
                        transactionTypeField.getText().trim(),
                        counterpartyField.getText().trim(),
                        commodityField.getText().trim(),
                        (String) editInOutComboBox.getSelectedItem(), // Get value from the edit dialog's ComboBox
                        safeParseDouble(paymentAmountField.getText().trim()),
                        paymentMethodField.getText().trim(),
                        currentStatusField.getText().trim(),
                        orderNumberField.getText().trim(), // Use the disabled field's text for ON (holds originalOrderNumber)
                        merchantNumberField.getText().trim(),
                        remarksField.getText().trim()
                );

                // Submit the AI task to the ExecutorService (from pre)
                executorService.submit(() -> { // Use submit (from pre)
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


            confirmButton.addActionListener(e -> {
                // Get values from dialog components
                String transactionTime = transactionTimeField.getText().trim();
                String finalTransactionType = transactionTypeField.getText().trim();
                String counterparty = counterpartyField.getText().trim();
                String commodity = commodityField.getText().trim();
                String inOut = (String) editInOutComboBox.getSelectedItem(); // GET VALUE FROM THE EDIT DIALOG'S COMBOBOX (from pre)
                String paymentAmountText = paymentAmountField.getText().trim();
                String paymentMethod = paymentMethodField.getText().trim();
                String currentStatus = currentStatusField.getText().trim();
                String orderNumber = orderNumberField.getText().trim(); // Get from disabled field, which holds original ON (from pre)
                String merchantNumber = merchantNumberField.getText().trim();
                String remarks = remarksField.getText().trim();

                // --- Input Validation --- (from pre)
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
                    // This check is theoretically redundant with the JComboBox but good practice
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
                // Check for non-negative amount consistency with In/Out (from pre)
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
                        paymentAmount, paymentMethod, currentStatus, originalOrderNumber, // Use the correct originalOrderNumber (captured at method start) (from pre)
                        merchantNumber, remarks
                );


                try {
                    transactionService.changeTransaction(updatedTransaction);
                    System.out.println("Edit successful. Preparing to refresh display filtered by InOut: " + updatedTransaction.getInOut());
                    clearSearchFields();
                    // Attempt to keep the current In/Out filter selected after refresh (from pre)
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

                    triggerCurrentSearch(); // Refresh the table based on potentially updated filters (from pre)

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

            // --- Dialog setup and showing --- (from pre)
            editDialog.pack();
            editDialog.setLocationRelativeTo(this);
            editDialog.setVisible(true); // Show the edit dialog
            // --- End Dialog setup ---

        } else {
            System.err.println("Attempted to edit row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }


    // Inside MenuUI class, createAIPanel method - MODIFIED (integrates seasonal analysis from post, keeps pre's batch button)
    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 通用分析面板 (原始数据) ---
        JPanel generalAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField userRequestField = new JTextField(40);
        aiStartTimeField = new JTextField(10); // 格式应为 yyyy/MM/dd HH:mm
        aiEndTimeField = new JTextField(10);   // 格式应为 yyyy/MM/dd HH:mm
        aiAnalyzeButton = new JButton("General Analysis (Raw Data)"); // "通用分析 (原始数据)"

        generalAnalysisPanel.add(new JLabel("General Analysis Request:")); // "通用分析请求:"
        generalAnalysisPanel.add(userRequestField);
        generalAnalysisPanel.add(new JLabel("Time Range (yyyy/MM/dd HH:mm): From:")); // "时间范围 (yyyy/MM/dd HH:mm): 从:"
        generalAnalysisPanel.add(aiStartTimeField);
        generalAnalysisPanel.add(new JLabel("To:")); // "到:"
        generalAnalysisPanel.add(aiEndTimeField); // 将 aiEndTimeField 添加到布局中
        generalAnalysisPanel.add(aiAnalyzeButton);

        // --- 基于月度总结的分析面板 ---
        JPanel summaryAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiPersonalSummaryButton = new JButton("Personal Spending Summary"); // "个人消费总结"
        aiSavingsGoalsButton = new JButton("Savings Goal Suggestions");   // "储蓄目标建议"
        aiPersonalSavingTipsButton = new JButton("Personalized Saving Tips"); // "个性化省钱技巧"
        // Initialize the NEW seasonal analysis button here (from post)
        aiSeasonalAnalysisButton = new JButton("Analyze Seasonal Spending (China Focus)"); // "分析季节性消费 (中国视角)" - Initialize new button

        summaryAnalysisPanel.add(new JLabel("Based on Monthly Summary Analysis:")); // "基于月度总结分析:"
        summaryAnalysisPanel.add(aiPersonalSummaryButton);
        summaryAnalysisPanel.add(aiSavingsGoalsButton);
        summaryAnalysisPanel.add(aiPersonalSavingTipsButton);
        summaryAnalysisPanel.add(aiSeasonalAnalysisButton); // ADD the new button to the panel (from post)

        // --- 学生专属功能面板 ---
        JPanel csButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiBudgetButton = new JButton("Budget Suggestion (Student)"); // "预算建议 (学生)"
        aiTipsButton = new JButton("Saving Tips (Student)");       // "省钱技巧 (学生)"

        csButtonsPanel.add(new JLabel("Student-Specific Features:")); // "学生专属功能:"
        csButtonsPanel.add(aiBudgetButton);
        csButtonsPanel.add(aiTipsButton);

        // --- NEW Panel for Batch/Debug Tasks --- (from pre)
        JPanel batchTaskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        runBatchAiButton = new JButton("Run Batch AI Analysis (Test ExecutorService)"); // Create the button
        batchTaskPanel.add(new JLabel("Multi-thread performace testing: ")); // Optional label
        batchTaskPanel.add(runBatchAiButton); // Add the button
        // --- End New Panel ---

        // --- 顶部控制面板 (所有按钮面板的布局) ---
        JPanel topControlPanel = new JPanel();
        topControlPanel.setLayout(new BoxLayout(topControlPanel, BoxLayout.Y_AXIS));
        topControlPanel.add(generalAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(summaryAnalysisPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(csButtonsPanel);
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topControlPanel.add(batchTaskPanel); // Add the new batch task panel (from pre)
        topControlPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing below it


        panel.add(topControlPanel, BorderLayout.NORTH);

        aiResultArea = new JTextArea();
        aiResultArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // "微软雅黑"
        aiResultArea.setLineWrap(true);
        aiResultArea.setWrapStyleWord(true);
        aiResultArea.setEditable(false);
        // Update initial text to include description for the new seasonal analysis button (from post)
        aiResultArea.setText("Welcome to the AI Personal Finance Analysis feature.\n\n" + // "欢迎使用AI个人财务分析功能。\n\n"
                "You can try the following operations:\n" + // "您可以尝试以下操作：\n"
                "1. Enter a general analysis request in the input field above (based on raw data, time range can be specified), then click \"General Analysis (Raw Data)\".\n" + // "1. 在上方输入框输入通用分析请求（基于原始数据，可指定时间范围），然后点击“通用分析”。\n"
                "2. Click \"Personal Spending Summary\" to get a detailed summary based on your monthly income and expenses.\n" + // "2. 点击“个人消费总结”获取基于您月度收支的详细总结。\n"
                "3. Click \"Savings Goal Suggestions\" to get savings advice based on your income and expenditure situation.\n" + // "3. 点击“储蓄目标建议”获取基于您收支情况的储蓄建议。\n"
                "4. Click \"Personalized Saving Tips\" to get saving advice based on your spending categories.\n" + // "4. 点击“个性化省钱技巧”获取基于您消费类别的省钱建议。\n"
                "5. Click \"Analyze Seasonal Spending (China Focus)\" for insights into your spending habits across different seasons/holidays.\n" + // "5. 点击“分析季节性消费 (中国视角)”获取您在不同季节/节假日的消费习惯洞察。\n" (新增说明 from post)
                "6. Student users can click \"Budget Suggestion (Student)\" and \"Saving Tips (Student)\" for exclusive advice.\n" + // "6. 学生用户可以点击“预算建议”和“省钱技巧”获取专属建议。\n"
                "7. (Admin only) Click \"Run Batch AI Analysis (Test ExecutorService)\" to test multi-threaded AI performance.\n"); // Added description for batch button (from pre)


        JScrollPane resultScrollPane = new JScrollPane(aiResultArea);
        panel.add(resultScrollPane, BorderLayout.CENTER);

        // Action listeners for AI buttons (from pre, modified/added where necessary)

        // Seasonal Analysis Button Action Listener - NEW (from post, adapted to use pre's ExecutorService)
        aiSeasonalAnalysisButton.addActionListener(e -> {
            aiResultArea.setText("--- Analyzing Seasonal Spending Patterns (China Focus) ---\n\nPlease wait while AI analyzes your monthly data for seasonal trends...\n"); // "--- 正在分析季节性消费模式 (中国视角) ---\n\n请稍候，AI正在分析您的月度数据以寻找季节性趋势...\n"
            setAIButtonsEnabled(false);
            // Use executorService from pre
            executorService.submit(() -> { // Use submit
                System.out.println("Seasonal Analysis task submitted to ExecutorService..."); // Optional logging
                String result = aiTransactionService.analyzeSeasonalSpendingPatterns(currentUser.getTransactionFilePath());
                System.out.println("Seasonal Analysis task finished."); // Optional logging
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText("--- Seasonal Spending Analysis (China Focus) ---\n\n" + result); // "--- 季节性消费分析 (中国视角) ---\n\n"
                    setAIButtonsEnabled(true);
                    System.out.println("UI updated after Seasonal Analysis task."); // Optional logging
                });
            });
        });


        // Action Listener for the Batch AI Button (from pre)
        runBatchAiButton.addActionListener(e -> {
            int numberOfTasks = 10; // Define the number of tasks for the batch run
            String userRequest = "Summarize recent activity"; // Example request for batch tasks
            String filePath = currentUser.getTransactionFilePath(); // Use current user's file path
            String startTime = "2024/01/01"; // Example time range for batch tasks
            String endTime = ""; // Example time range

            System.out.println("Starting batch AI analysis (" + numberOfTasks + " tasks) via ExecutorService...");

            // Clear previous results and show a loading message
            aiResultArea.setText("Running batch AI analysis (" + numberOfTasks + " tasks), please wait...\n");
            // Disable all AI related buttons while batch is running
            setAIButtonsEnabled(false);

            // Use an AtomicInteger to track completed tasks across threads (from pre)
            java.util.concurrent.atomic.AtomicInteger completedTasks = new java.util.concurrent.atomic.AtomicInteger(0);
            long startTimeMillis = System.currentTimeMillis(); // Record start time for total duration

            for (int i = 0; i < numberOfTasks; i++) {
                final int taskIndex = i;
                // Submit each individual task to the ExecutorService (from pre)
                executorService.submit(() -> { // Use submit
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

                        // Update UI with progress and final status on EDT (from pre)
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

        // Personal Spending Summary Button (from pre, uses ExecutorService)
        aiPersonalSummaryButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personal Spending Summary ---\n\nGenerating summary based on your monthly spending data, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
                String result = aiTransactionService.generatePersonalSummary(currentUser.getTransactionFilePath()); // Call the method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Personal Spending Summary ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // Savings Goal Suggestions Button (from pre, uses ExecutorService)
        aiSavingsGoalsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Savings Goal Suggestions ---\n\nGenerating savings goal suggestions based on your income and expenses, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
                String result = aiTransactionService.suggestSavingsGoals(currentUser.getTransactionFilePath()); // Call the method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Savings Goal Suggestions ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // Personalized Saving Tips Button (from pre, uses ExecutorService)
        aiPersonalSavingTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Personalized Saving Tips ---\n\nGenerating saving tips based on your spending categories, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
                String result = aiTransactionService.givePersonalSavingTips(currentUser.getTransactionFilePath()); // Call the method
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- Personalized Saving Tips ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // General Analysis Button (from pre, uses ExecutorService)
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
            executorService.submit(() -> { // Use submit
                String result = aiTransactionService.analyzeTransactions(userRequest, currentUser.getTransactionFilePath(), startTimeStr, endTimeStr);
                SwingUtilities.invokeLater(() -> { // Update UI on EDT
                    aiResultArea.setText("--- General Analysis Result ---\n\n" + result);
                    setAIButtonsEnabled(true);
                });
            });
        });

        // College Student Budget Button (from pre, uses ExecutorService)
        aiBudgetButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Budget Suggestion ---\n\nGenerating budget suggestion based on your historical spending, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
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

        // College Student Tips Button (from pre, uses ExecutorService)
        aiTipsButton.addActionListener(e -> {
            aiResultArea.setText("--- Generating Student Saving Tips ---\n\nGenerating saving tips, please wait...\n");
            setAIButtonsEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
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
     * Includes the new seasonal and batch buttons.
     * (from pre, modified to include seasonal button)
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
        if (runBatchAiButton != null) runBatchAiButton.setEnabled(enabled); // Include the batch button (from pre)
        if (aiSeasonalAnalysisButton != null) aiSeasonalAnalysisButton.setEnabled(enabled); // Include the new button (from post)

    }


    // Inside MenuUI class, createAdminStatsPanel method - (from pre, uses ExecutorService)
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

        // Generate Stats button listener (Uses ExecutorService) (from pre)
        generateStatsButton.addActionListener(e -> {
            adminStatsArea.setText("Generating/Updating summary statistics, please wait...\n"); // "Generating/Updating summary statistics, please wait...\n"
            generateStatsButton.setEnabled(false);
            refreshDisplayButton.setEnabled(false);

            // Submit task to ExecutorService
            executorService.submit(() -> { // Use submit
                String message;
                try {
                    // This call might generate stats for all users, depending on SummaryStatisticService implementation
                    summaryStatisticService.generateAndSaveWeeklyStatistics(); // Call the method
                    message = "Summary statistics generated/updated successfully!\nPlease click 'Refresh Display' to view the latest data."; // "Summary statistics generated/updated successfully!\nPlease click 'Refresh Display' to view the latest data."
                    System.out.println("Generate Stats task finished: " + message);
                } catch (Exception ex) {
                    message = "Failed to generate/update summary statistics!\n" + ex.getMessage(); // "Failed to generate/update summary statistics!\n"
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

        // Refresh Display button listener (Uses ExecutorService) (from pre)
        refreshDisplayButton.addActionListener(e -> {
            displaySummaryStatistics(); // This method itself submits a task
        });

        // Initial display when the panel is first shown (from pre)
        executorService.submit(() -> { // Use submit
            System.out.println("Initial Admin Stats load task submitted to ExecutorService...");
            SwingUtilities.invokeLater(() -> adminStatsArea.setText("Loading existing statistics...\n")); // "Loading existing statistics...\n"
            try {
                // This loads data from the configured summary file path (likely admin's own or global)
                List<SummaryStatistic> initialStats = summaryStatisticService.getAllSummaryStatistics(); // Call the method
                System.out.println("Initial Admin Stats load task finished. Found " + initialStats.size() + " stats.");
                if (!initialStats.isEmpty()) {
                    // If initial stats exist, trigger display (which submits another EDT task)
                    SwingUtilities.invokeLater(this::displaySummaryStatistics); // This triggers display using the loaded data (indirectly)
                } else {
                    SwingUtilities.invokeLater(() -> adminStatsArea.setText("No existing summary statistics found.\nPlease click the 'Generate/Update Statistics' button to generate them.")); // "No existing summary statistics found.\nPlease click the 'Generate/Update Statistics' button to generate them."
                }
            } catch (IOException ex) {
                System.err.println("Initial Admin Stats load task failed: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> adminStatsArea.setText("Failed to load existing statistics!\n" + ex.getMessage())); // "Failed to load existing statistics!\n"
            }
        });

        return panel;
    }

    // Inside MenuUI class, displaySummaryStatistics method - (from pre, uses ExecutorService)
    private void displaySummaryStatistics() {
        adminStatsArea.setText("Loading summary statistics...\n"); // "Loading summary statistics...\n"
        if(generateStatsButton != null) generateStatsButton.setEnabled(false);
        if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(false);

        // Submit data loading task to ExecutorService (from pre)
        executorService.submit(() -> { // Use submit
            String displayContent;
            try {
                // This loads data from the configured summary file path (likely admin's own or global)
                List<SummaryStatistic> stats = summaryStatisticService.getAllSummaryStatistics(); // Call the method
                if (stats.isEmpty()) {
                    displayContent = "No summary statistics currently available.\nPlease click the 'Generate/Update Statistics' button first."; // "No summary statistics currently available.\nPlease click the 'Generate/Update Statistics' button first."
                } else {
                    StringBuilder sb = new StringBuilder("===== Summary Statistics =====\n\n"); // "===== Summary Statistics =====\n\n"
                    // Sort stats by week identifier (from pre)
                    stats.sort(Comparator.comparing(SummaryStatistic::getWeekIdentifier));
                    // Display in reverse chronological order (latest first) (from pre)
                    for (int i = stats.size() - 1; i >= 0; i--) {
                        SummaryStatistic stat = stats.get(i);
                        sb.append("Week Identifier: ").append(stat.getWeekIdentifier()).append("\n"); // "Week Identifier: "
                        // NOTE: These getters reflect the structure in the pre.txt SummaryStatistic model
                        sb.append("  Total Income (All Users): ").append(String.format("%.2f", stat.getTotalIncomeAllUsers())).append(" CNY\n"); // "  Total Income: " ... " CNY\n"
                        sb.append("  Total Expense (All Users): ").append(String.format("%.2f", stat.getTotalExpenseAllUsers())).append(" CNY\n"); // "  Total Expense: " ... " CNY\n"
                        if (stat.getTopExpenseCategoryAmount() > 0) {
                            sb.append("  Top Expense Category (All Users): ").append(stat.getTopExpenseCategory()).append(" (").append(String.format("%.2f", stat.getTopExpenseCategoryAmount())).append(" CNY)\n"); // "  Top Expense Category: " ... " CNY)\n"
                        } else {
                            sb.append("  Top Expense Category (All Users): No significant expense category\n"); // "  Top Expense Category: No significant expense category\n"
                        }
                        // If SummaryStatistic model includes numberOfUsersWithTransactions
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
            SwingUtilities.invokeLater(() -> { // Update UI on EDT
                adminStatsArea.setText(finalDisplayContent);
                if(generateStatsButton != null) generateStatsButton.setEnabled(true);
                if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(true);
            });
        });
    }

    // Inside MenuUI class, deleteRow method - (from pre, uses ExecutorService)
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
                    "Are you sure you want to delete the transaction with order number '" + orderNumber + "'?", // "Are you sure you want to delete the transaction with order number '" ... "'?"
                    "Confirm Delete", // "Confirm Delete"
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // Submit deletion task to the ExecutorService (from pre)
                String finalOrderNumber = orderNumber; // Final variable for lambda
                executorService.submit(() -> { // Use submit
                    System.out.println("Delete task submitted to ExecutorService for ON: " + finalOrderNumber);
                    String message;
                    boolean deleted = false;
                    try {
                        deleted = transactionService.deleteTransaction(finalOrderNumber); // Call the method
                        if (deleted) {
                            message = "Delete successful!"; // "Delete successful!"
                            System.out.println("Delete task finished: " + message);
                        } else {
                            message = "Delete failed: Corresponding order number " + finalOrderNumber + " not found."; // "Delete failed: Corresponding order number " ... " not found."
                            System.err.println("Delete task failed: " + message);
                        }
                    } catch (Exception ex) {
                        message = "Delete failed!\n" + ex.getMessage(); // "Delete failed!\n"
                        System.err.println("Delete task failed: " + ex.getMessage());
                        ex.printStackTrace();
                    }

                    String finalMessage = message;
                    boolean finalDeleted = deleted; // Final variable for lambda
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        System.out.println("Updating UI on EDT after Delete task.");
                        if (finalDeleted) {
                            // Remove the row from the table model directly if deletion was successful
                            // Find the row index again, as it might have changed (from pre logic)
                            int currentRowIndex = -1;
                            for(int i=0; i < this.tableModel.getRowCount(); i++) {
                                Object cellValue = this.tableModel.getValueAt(i, 8); // Order Number is at index 8
                                if (cellValue != null && finalOrderNumber.equals(((String) cellValue).trim())) {
                                    currentRowIndex = i;
                                    break;
                                }
                            }
                            if (currentRowIndex != -1) {
                                this.tableModel.removeRow(currentRowIndex);
                            } else {
                                // If row wasn't found in the model, just reload all data to be safe.
                                // Reload all or trigger current search (from pre)
                                triggerCurrentSearch();
                            }

                            JOptionPane.showMessageDialog(null, finalMessage, "Information", JOptionPane.INFORMATION_MESSAGE); // "Information"
                            System.out.println("UI update complete after Delete task. Row removed.");

                            // After deletion, trigger a refresh of the displayed data based on current search criteria. (from pre)
                            // triggerCurrentSearch(); // Already called above if row wasn't found, or implicitly done by removing row
                        } else {
                            // If deletion failed in service (e.g., not found), just show message.
                            JOptionPane.showMessageDialog(null, finalMessage, "Error", JOptionPane.ERROR_MESSAGE); // "Error"
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

    // Inside MenuUI class, createRowFromTransaction method - same as before (from pre)
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

    // Inside MenuUI class, searchData method - (from pre, uses ExecutorService)
    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) {
        System.out.println("Searching with criteria: time='" + query1 + "', type='" + query2 + "', counterparty='" + query3 + "', commodity='" + query4 + "', inOut='" + query6 + "', paymentMethod='" + query5 + "'");
        this.tableModel.setRowCount(0); // Clear the current table display

        Transaction searchCriteria = new Transaction(
                query1, query2, query3, query4, query6,
                0, // Amount is not a search criteria from the UI input fields
                query5,
                "", "", "", "" // Other fields are not searchable from the UI input fields
        );

        // Submit search task to the ExecutorService (from pre)
        executorService.submit(() -> { // Use submit
            System.out.println("Search task submitted to ExecutorService.");
            try {
                List<Transaction> transactions = transactionService.searchTransaction(searchCriteria); // Call the method
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
                String errorMessage = "Search failed!\n" + ex.getMessage(); // "Search failed!\n"
                SwingUtilities.invokeLater(() -> { // Show error message on EDT
                    JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE); // "Error"
                    System.out.println("UI update complete after Search task. Error message shown.");
                });
            }
        });
    }


    // Helper method to safely parse a double (from pre)
    private double safeParseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { System.err.println("Failed to parse double from string: '" + value + "'"); return 0.0; }
    }

    // Helper method to clear search fields (from pre)
    private void clearSearchFields() {
        searchTransactionTimeField.setText("");
        searchTransactionTypeField.setText("");
        searchCounterpartyField.setText("");
        searchCommodityField.setText("");
        searchInOutComboBox.setSelectedItem("");
        searchPaymentMethodField.setText("");
        System.out.println("Cleared search fields.");
    }
    // Helper method to trigger search (from pre)
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
    // Helper method for null check (from pre)
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
    // Getter for the table (from pre)
    public JTable getTable() {
        return table;
    }

    // Note: ButtonRenderer and ButtonEditor classes are assumed to be defined elsewhere or in inner classes,
    // as they are used but not defined in the provided text.
}