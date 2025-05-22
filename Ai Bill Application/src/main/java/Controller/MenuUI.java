package Controller;

import Constants.ConfigConstants;
// Remove import DAO.CsvTransactionDao;

import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Utils.CacheManager;
import model.Transaction;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector; // Still used for table row data structure
import javax.swing.table.DefaultTableModel;

// Remove static imports related to CSV_PATH

public class MenuUI {
    private final User currentUser;
    private final TransactionService transactionService;

    // Make tableModel an instance field
    private DefaultTableModel tableModel;
    // Remove the static or instance allData field: private Vector<Vector<String>> allData = new Vector<>();

    // Add instance fields for search input components (for Question 2 & 3)
    private JTextField searchTransactionTimeField;
    private JTextField searchTransactionTypeField;
    private JTextField searchCounterpartyField;
    private JTextField searchCommodityField;
    private JComboBox<String> searchInOutComboBox;
    private JTextField searchPaymentMethodField;
    private JButton searchButton; // Keep reference to the search button

    private JTable table;
    private HistogramPanelContainer histogramPanelContainer;
    private JPanel rightPanel;
    private CardLayout cardLayout;

    // Constructor now accepts the authenticated User and their TransactionService instance
    public MenuUI(User authenticatedUser, TransactionService transactionService){
        this.currentUser = authenticatedUser;
        this.transactionService = transactionService; // Inject the user-specific service

        // Initialize table model (now non-static)
        String[] columnNames = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注", "Modify", "Delete"};
        this.tableModel = new DefaultTableModel(columnNames, 0);
        this.table = new JTable(this.tableModel);

        this.histogramPanelContainer = new HistogramPanelContainer();
        this.cardLayout = new CardLayout();
        this.rightPanel = new JPanel(this.cardLayout);

        // DEBUG: Print user info
        System.out.println("MenuUI initialized for user: " + currentUser.getUsername());
        System.out.println("Using transaction file: " + currentUser.getTransactionFilePath());

        // Data loading will be called in createMainPanel() after UI setup
    }

    public JPanel createMainPanel() {
        // Main panel, use BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Left panel with Menu and AI buttons
        JPanel leftPanel = createLeftPanel();
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Right panel for table or AI view
        setupRightPanel();
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        // Load the user's data and display initial view (only income)
        loadCSVDataForCurrentUser("收入"); // Load and display only income initially

        return mainPanel;
    }

    // Method to load CSV data for the current user with optional initial filter
    // This replaces the old loadCSVDataForCurrentUser() method
    public void loadCSVDataForCurrentUser(String initialInOutFilter) {
        this.tableModel.setRowCount(0); // Clear the table model

        try {
            // Get all transactions for the current user using the injected service
            List<Transaction> transactions = transactionService.getAllTransactions();
            System.out.println("Loaded total " + transactions.size() + " transactions from service for user " + currentUser.getUsername());

            // Filter transactions based on the initialInOutFilter
            List<Transaction> filteredTransactions = new java.util.ArrayList<>();
            if (initialInOutFilter == null || initialInOutFilter.trim().isEmpty()) {
                // If no filter specified, add all transactions
                filteredTransactions.addAll(transactions);
            } else {
                // Filter by the specified 收/支 type
                String filter = initialInOutFilter.trim();
                filteredTransactions = transactions.stream()
                        .filter(t -> t.getInOut() != null && (t.getInOut().equalsIgnoreCase(filter) ||
                                (filter.equalsIgnoreCase("收入") && t.getInOut().equalsIgnoreCase("收")) ||
                                (filter.equalsIgnoreCase("支出") && t.getInOut().equalsIgnoreCase("支")) ))
                        .collect(java.util.stream.Collectors.toList());
            }


            // Add filtered transactions to the table model
            for (Transaction transaction : filteredTransactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                this.tableModel.addRow(row);
            }
            System.out.println("Displayed " + filteredTransactions.size() + " transactions in the table.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "加载用户交易数据失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to create the left panel (Menu/AI buttons) - no changes needed here // [cite: 274]
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new GridLayout(2, 1)); // [cite: 384]
        JButton menuButton = new JButton("Menu"); // [cite: 385]
        JButton aiButton = new JButton("AI"); // [cite: 385]

        leftPanel.add(menuButton); // [cite: 385]
        leftPanel.add(aiButton); // [cite: 386]
        // 为 Menu 按钮添加 ActionListener // [cite: 386]
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table"); // 切换到表格界面 // [cite: 386]
        });
        // 为 AI 按钮添加 ActionListener // [cite: 387]
        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Histogram"); // 切换到直方图界面 // [cite: 387]
        });
        return leftPanel; // [cite: 388]
    }


    // Method to set up the right panel (Table/Histogram) - no changes needed here // [cite: 275]
    private void setupRightPanel() {
        // 创建搜索和表格的面板 // [cite: 388]
        JPanel tablePanel = createTablePanel(); // [cite: 389]
        // 将表格面板和直方图面板添加到 rightPanel // [cite: 389]
        rightPanel.add(tablePanel, "Table"); // [cite: 389]
        rightPanel.add(histogramPanelContainer, "Histogram"); // [cite: 390]
    }

    // Method to create the table panel - update button editors/renderers to use 'this' MenuUI instance
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        JPanel inputPanel = createInputPanel(); // This method now initializes search fields
        tablePanel.add(inputPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(this.table);
        tableScrollPane.setPreferredSize(new Dimension(1000, 250));
        this.table.setRowHeight(30);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Set cell renderers and editors - pass 'this' MenuUI instance
        this.table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(this));

        this.table.getColumnModel().getColumn(12).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(12).setCellEditor(new ButtonEditor(this));

        // Data loading is now called in createMainPanel()

        return tablePanel;
    }

    // Method to create input panel - Capture references to search fields and button (for Question 2 & 3)
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // Create input fields and capture references
        searchTransactionTimeField = new JTextField(10); // 交易时间输入框
        searchTransactionTypeField = new JTextField(10); // 交易类型输入框
        searchCounterpartyField = new JTextField(10);    // 交易对方输入框
        searchCommodityField = new JTextField(10);      // 商品输入框
        searchInOutComboBox = new JComboBox<>(new String[]{"", "收入", "支出"}); // Add empty option for "any"
        searchPaymentMethodField = new JTextField(10);  // 支付方式输入框

        // Add labels and input fields
        inputPanel.add(new JLabel("交易时间:"));
        inputPanel.add(searchTransactionTimeField);
        inputPanel.add(new JLabel("交易类型:"));
        inputPanel.add(searchTransactionTypeField);
        inputPanel.add(new JLabel("交易对方:"));
        inputPanel.add(searchCounterpartyField);
        inputPanel.add(new JLabel("商品:"));
        inputPanel.add(searchCommodityField);
        inputPanel.add(new JLabel("收/支:"));
        inputPanel.add(searchInOutComboBox);
        inputPanel.add(new JLabel("支付方式:"));
        inputPanel.add(searchPaymentMethodField);

        // Create Search and Add buttons and capture reference to search button
        searchButton = new JButton("Search");
        JButton addButton = new JButton("Add");

        // Add buttons
        inputPanel.add(searchButton);
        inputPanel.add(addButton);

        // Add ActionListener for Search button
        searchButton.addActionListener(e -> {
            // Call searchData with current values from the input fields
            searchData(
                    searchTransactionTimeField.getText().trim(),
                    searchTransactionTypeField.getText().trim(),
                    searchCounterpartyField.getText().trim(),
                    searchCommodityField.getText().trim(),
                    (String) searchInOutComboBox.getSelectedItem(),
                    searchPaymentMethodField.getText().trim()
            );
        });

        // Add ActionListener for Add button
        addButton.addActionListener(e -> {
            showAddTransactionDialog();
        });

        return inputPanel;
    }

    // Method to show add transaction dialog - update to use injected service
    private void showAddTransactionDialog() {
        // ... existing code to create dialog and fields ...
        JDialog addDialog = new JDialog();
        addDialog.setTitle("添加交易");
        addDialog.setLayout(new GridLayout(12, 2));

        JTextField transactionTimeField = new JTextField();
        JTextField transactionTypeField = new JTextField();
        JTextField counterpartyField = new JTextField();
        JTextField commodityField = new JTextField();
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"收入", "支出"}); // Or maybe allow empty for "other"?
        JTextField paymentAmountField = new JTextField();
        JTextField paymentMethodField = new JTextField();
        JTextField currentStatusField = new JTextField();
        JTextField orderNumberField = new JTextField();
        JTextField merchantNumberField = new JTextField();
        JTextField remarksField = new JTextField();

        addDialog.add(new JLabel("交易时间:")); addDialog.add(transactionTimeField);
        addDialog.add(new JLabel("交易类型:")); addDialog.add(transactionTypeField);
        addDialog.add(new JLabel("交易对方:")); addDialog.add(counterpartyField);
        addDialog.add(new JLabel("商品:")); addDialog.add(commodityField);
        addDialog.add(new JLabel("收/支:")); addDialog.add(inOutComboBox);
        addDialog.add(new JLabel("金额(元):")); addDialog.add(paymentAmountField);
        addDialog.add(new JLabel("支付方式:")); addDialog.add(paymentMethodField);
        addDialog.add(new JLabel("当前状态:")); addDialog.add(currentStatusField);
        addDialog.add(new JLabel("交易单号:")); addDialog.add(orderNumberField);
        addDialog.add(new JLabel("商户单号:")); addDialog.add(merchantNumberField);
        addDialog.add(new JLabel("备注:")); addDialog.add(remarksField);


        // Add confirm button
        JButton confirmButton = new JButton("确认");
        confirmButton.addActionListener(e -> {
            String paymentAmountText = paymentAmountField.getText().trim();
            double paymentAmount = 0.0;
            if (!paymentAmountText.isEmpty()) {
                try {
                    paymentAmount = Double.parseDouble(paymentAmountText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(addDialog, "金额格式不正确！请输入数字。", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Transaction newTransaction = new Transaction(
                    emptyIfNull(transactionTimeField.getText().trim()),
                    emptyIfNull(transactionTypeField.getText().trim()),
                    emptyIfNull(counterpartyField.getText().trim()),
                    emptyIfNull(commodityField.getText().trim()),
                    (String) inOutComboBox.getSelectedItem(),
                    paymentAmount,
                    emptyIfNull(paymentMethodField.getText().trim()),
                    emptyIfNull(currentStatusField.getText().trim()),
                    emptyIfNull(orderNumberField.getText().trim()), // Ensure order number is provided
                    emptyIfNull(merchantNumberField.getText().trim()),
                    emptyIfNull(remarksField.getText().trim())
            );

            if (newTransaction.getOrderNumber().isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "交易单号不能为空！", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                transactionService.addTransaction(newTransaction);

                // After adding, decide what to display.
                // Option 1: Reload the default (income only) view:
                // loadCSVDataForCurrentUser("收入");
                // Option 2: Reload all data:
                // loadCSVDataForCurrentUser("");
                // Option 3: Reload the *current* search/filter view (more complex, requires storing current criteria)
                // Let's choose Option 2 for simplicity - show all data after adding a new transaction.
                // The user can then filter if needed.
                loadCSVDataForCurrentUser(""); // Load all data after adding

                // Also clear search fields after adding
                clearSearchFields();


                addDialog.dispose();
                JOptionPane.showMessageDialog(null, "交易添加成功！", "提示", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "交易添加失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        addDialog.add(confirmButton);
        addDialog.setSize(400, 300);
        addDialog.setModal(true);
        addDialog.setVisible(true);
    }


    // Method to search data - update to clear tableModel only (remove allData usage)
    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) { // Renamed query params for clarity
        System.out.println("Searching with criteria: time='" + query1 + "', type='" + query2 + "', counterparty='" + query3 + "', commodity='" + query4 + "', inOut='" + query6 + "', paymentMethod='" + query5 + "'");
        this.tableModel.setRowCount(0); // Clear the table model

        // Create search criteria Transaction object
        Transaction searchCriteria = new Transaction(
                query1, query2, query3, query4, query6,
                0, // paymentAmount not used in search criteria from UI
                query5,
                "", "", "", ""
        );

        try {
            // Use the injected transactionService instance
            List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);
            System.out.println("Search results count: " + transactions.size());
            for (Transaction transaction : transactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                this.tableModel.addRow(row);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "查询失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to create table row from Transaction object - no changes needed here
    private Vector<String> createRowFromTransaction(Transaction transaction) {
        Vector<String> row = new Vector<>();
        row.add(emptyIfNull(transaction.getTransactionTime()));
        row.add(emptyIfNull(transaction.getTransactionType()));
        row.add(emptyIfNull(transaction.getCounterparty()));
        row.add(emptyIfNull(transaction.getCommodity()));
        row.add(emptyIfNull(transaction.getInOut()));
        row.add(String.valueOf(transaction.getPaymentAmount())); // Keep as String for table
        row.add(emptyIfNull(transaction.getPaymentMethod()));
        row.add(emptyIfNull(transaction.getCurrentStatus()));
        row.add(emptyIfNull(transaction.getOrderNumber()));
        row.add(emptyIfNull(transaction.getMerchantNumber()));
        row.add(emptyIfNull(transaction.getRemarks()));
        row.add("Modify"); // Modify button text
        row.add("Delete"); // Delete button text
        return row;
    }


    // Method to delete row - get data from tableModel (remove allData usage)
    public void deleteRow(int rowIndex) {
        System.out.println("尝试删除行: " + rowIndex + " for user " + currentUser.getUsername());
        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) { // Use tableModel row count

            // Get the order number directly from the displayed table row
            String orderNumber = (String) this.tableModel.getValueAt(rowIndex, 8); // OrderNumber is at index 8
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "无法删除：交易单号为空！", "错误", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to delete row " + rowIndex + " but order number is null or empty.");
                return; // Cannot delete without an order number
            }
            orderNumber = orderNumber.trim();
            System.out.println("Deleting transaction with order number: " + orderNumber);

            try {
                // Use the injected transactionService instance
                boolean deleted = transactionService.deleteTransaction(orderNumber);

                if (deleted) {
                    // Data is removed from CSV and cache invalidated by service.
                    // Update the UI model directly by removing the row.
                    this.tableModel.removeRow(rowIndex); // Remove the row from the displayed table


                    JOptionPane.showMessageDialog(null, "删除成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Deleted row " + rowIndex + " with order number " + orderNumber + " from UI.");

                    // After delete, refresh the view. Let's reload the *current* search/filter view.
                    // Get the current search criteria from the UI fields and re-apply search.
                    triggerCurrentSearch();

                } else {
                    // This case means the service said it wasn't deleted (likely not found)
                    JOptionPane.showMessageDialog(null, "删除失败：未找到对应的交易单号 " + orderNumber, "错误", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Delete failed: order number " + orderNumber + " not found by service.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "删除失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                System.err.println("Error during deletion for order number " + orderNumber);
            }
        } else {
            System.err.println("Attempted to delete row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    // Method to edit row - get data from tableModel and update display after edit (remove allData usage)
    public void editRow(int rowIndex) {
        System.out.println("编辑行: " + rowIndex + " for user " + currentUser.getUsername());
        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) { // Use tableModel row count

            // Get the data for the displayed row directly from the table model
            Vector<String> rowData = new Vector<>();
            // Get data for all columns that correspond to Transaction fields (up to index 10)
            for (int i = 0; i <= 10; i++) { // Columns 0 to 10 are Transaction fields
                Object value = this.tableModel.getValueAt(rowIndex, i);
                rowData.add(value != null ? value.toString() : "");
            }
            System.out.println("Retrieved row data from table model for editing: " + rowData);

            // Get the original order number, which is the key for update
            String originalOrderNumber = rowData.get(8).trim(); // Index 8 is OrderNumber
            if (originalOrderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(null, "无法编辑：交易单号为空！", "错误", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to edit row " + rowIndex + " but order number is empty.");
                return; // Cannot edit without an order number
            }


            // Create a panel for edit fields
            JPanel panel = new JPanel(new GridLayout(11, 2)); // 11 Transaction fields

            // Create fields array, populate panel and fields
            JTextField[] fields = new JTextField[11]; // 11 fields for Transaction data
            String[] fieldNames = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};

            for (int i = 0; i < fieldNames.length; i++) {
                panel.add(new JLabel(fieldNames[i] + ":")); // Add label
                fields[i] = new JTextField(rowData.get(i)); // Set field value from row data
                panel.add(fields[i]);
            }

            // Disable editing the order number field if it's the primary key and shouldn't be changed
            // If you decide OrderNumber is immutable via edit:
            fields[8].setEditable(false); // Disable editing OrderNumber field


            // Show the dialog
            int result = JOptionPane.showConfirmDialog(null, panel, "修改交易信息 (订单号: " + originalOrderNumber + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                // User clicked OK, get updated values from fields

                // Handle paymentAmount parsing carefully
                double paymentAmount = safeParseDouble(fields[5].getText().trim()); // 金额(元)

                // Create a Transaction object with updated values
                // Use the original order number as the key for the update operation
                Transaction updatedTransaction = new Transaction(
                        fields[0].getText().trim(), // 交易时间
                        fields[1].getText().trim(), // 交易类型
                        fields[2].getText().trim(), // 交易对方
                        fields[3].getText().trim(), // 商品
                        fields[4].getText().trim(), // 收/支
                        paymentAmount,
                        fields[6].getText().trim(), // 支付方式
                        fields[7].getText().trim(), // 当前状态
                        originalOrderNumber, // Use the ORIGINAL order number as the identifier for update
                        fields[9].getText().trim(), // 商户单号
                        fields[10].getText().trim() // 备注
                );

                try {
                    // Use the injected transactionService instance to change the transaction
                    transactionService.changeTransaction(updatedTransaction);

                    // *** IMPORTANT CHANGE FOR QUESTION 2 ***
                    // After successful edit, update the search fields and trigger search
                    System.out.println("Edit successful. Preparing to refresh display filtered by InOut: " + updatedTransaction.getInOut());
                    // Clear all search text fields
                    clearSearchFields();
                    // Set the InOut dropdown to the updated value
                    searchInOutComboBox.setSelectedItem(updatedTransaction.getInOut());
                    // Trigger the search based on the updated criteria
                    triggerCurrentSearch();


                    JOptionPane.showMessageDialog(null, "修改成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Edited row " + rowIndex + " for order number " + originalOrderNumber + " and refreshed display.");

                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(null, "修改失败！\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "修改失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error during editing for order number " + originalOrderNumber);
                }
            } else {
                System.out.println("Edit dialog cancelled.");
            }
        } else {
            System.err.println("Attempted to edit row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    // Helper method to safely parse double, return 0.0 on error
    private double safeParseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse double from string: '" + value + "'");
            // Consider showing a warning message to the user here
            return 0.0;
        }
    }

    // Helper method to clear all search input fields
    private void clearSearchFields() {
        searchTransactionTimeField.setText("");
        searchTransactionTypeField.setText("");
        searchCounterpartyField.setText("");
        searchCommodityField.setText("");
        searchInOutComboBox.setSelectedItem(""); // Set to the empty option
        searchPaymentMethodField.setText("");
        System.out.println("Cleared search fields.");
    }

    // Helper method to trigger search based on current search field values
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
     * If field is null, return empty string.
     * @param value Field value
     * @return Non-null field value
     */
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    // Keep getTable() for testing
    public JTable getTable() {
        return table;
    }
}