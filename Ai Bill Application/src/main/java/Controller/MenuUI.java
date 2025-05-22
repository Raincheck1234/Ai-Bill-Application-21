package Controller;

import Constants.ConfigConstants;
import Constants.StandardCategories; // Import StandardCategories if needed in UI
import DAO.TransactionDao; // Import if needed
import DAO.SummaryStatisticDao; // Import if needed
import Service.AIservice.AITransactionService; // Import AI services
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.TransactionServiceImpl;
import Service.Impl.SummaryStatisticService; // Import SummaryStatisticService
import Service.TransactionService;
import Utils.CacheManager;
import model.SummaryStatistic; // Import SummaryStatistic
import model.Transaction;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Comparator; // For sorting stats display

public class MenuUI extends JPanel { // Extend JPanel for easier use in Main (optional but common)

    private final User currentUser;
    private final TransactionService transactionService;
    private final SummaryStatisticService summaryStatisticService; // Add service for statistics
    private final AITransactionService aiTransactionService; // Add AI service instance
    private final CollegeStudentNeeds collegeStudentNeeds; // Add CollegeStudentNeeds instance

    private DefaultTableModel tableModel;
    // Removed allData field

    // Fields for search input components
    private JTextField searchTransactionTimeField;
    private JTextField searchTransactionTypeField;
    private JTextField searchCounterpartyField;
    private JTextField searchCommodityField;
    private JComboBox<String> searchInOutComboBox;
    private JTextField searchPaymentMethodField;
    private JButton searchButton;

    private JTable table;
    // private HistogramPanelContainer histogramPanelContainer; // Replaced by specific AI panel

    private JPanel rightPanel;
    private CardLayout cardLayout;

    // UI components for AI panel
    private JTextArea aiResultArea;
    private JTextField aiStartTimeField;
    private JTextField aiEndTimeField;
    private JButton aiAnalyzeButton;
    // Add College Student Needs buttons if needed
    private JButton aiBudgetButton;
    private JButton aiTipsButton;


    // UI components for Admin Stats panel
    private JTextArea adminStatsArea;
    private JButton generateStatsButton;
    private JButton refreshDisplayButton;


    // Constructor now accepts User, TransactionService, and SummaryStatisticService
    public MenuUI(User authenticatedUser, TransactionService transactionService, SummaryStatisticService summaryStatisticService){
        this.currentUser = authenticatedUser;
        this.transactionService = transactionService;
        this.summaryStatisticService = summaryStatisticService; // Save the service
        this.aiTransactionService = new AITransactionService(); // Initialize AI services
        this.collegeStudentNeeds = new CollegeStudentNeeds(); // Initialize CollegeStudentNeeds

        // Initialize table model
        String[] columnNames = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注", "Modify", "Delete"};
        this.tableModel = new DefaultTableModel(columnNames, 0);
        this.table = new JTable(this.tableModel);

        // Initialize the main panel layout
        setLayout(new BorderLayout()); // MenuUI itself is the main panel

        // Add the left panel
        add(createLeftPanel(), BorderLayout.WEST);

        // Add the right panel (which uses CardLayout)
        setupRightPanel();
        add(rightPanel, BorderLayout.CENTER);


        // Load the user's data and display initial view (only income)
        // loadCSVDataForCurrentUser("收入"); // Initial load happens after setupRightPanel in createMainPanel

        System.out.println("MenuUI initialized for user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
    }

    // createMainPanel method is no longer needed if MenuUI itself is the main JPanel
     public JPanel createMainPanel() { return this; }


    // Method to load CSV data for the current user with optional initial filter
    // Same logic as before
    public void loadCSVDataForCurrentUser(String initialInOutFilter) {
        // ... existing implementation ...
        this.tableModel.setRowCount(0); // Clear the table model

        try {
            List<Transaction> transactions = transactionService.getAllTransactions();
            System.out.println("Loaded total " + transactions.size() + " transactions from service for user " + currentUser.getUsername());

            List<Transaction> filteredTransactions = new java.util.ArrayList<>();
            if (initialInOutFilter == null || initialInOutFilter.trim().isEmpty()) {
                filteredTransactions.addAll(transactions);
            } else {
                String filter = initialInOutFilter.trim();
                filteredTransactions = transactions.stream()
                        .filter(t -> t.getInOut() != null && (t.getInOut().equalsIgnoreCase(filter) ||
                                (filter.equalsIgnoreCase("收入") && t.getInOut().equalsIgnoreCase("收")) ||
                                (filter.equalsIgnoreCase("支出") && t.getInOut().equalsIgnoreCase("支")) ))
                        .collect(java.util.stream.Collectors.toList());
            }

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

    // Method to create the left panel (Menu/AI/Admin buttons)
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical alignment
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add some padding

        JButton menuButton = new JButton("交易列表"); // More descriptive text
        JButton aiButton = new JButton("AI分析");
        JButton adminButton = new JButton("管理员统计"); // Admin button

        // Set consistent size for buttons (optional, but good UI practice)
        Dimension buttonSize = new Dimension(150, 40);
        menuButton.setMaximumSize(buttonSize);
        aiButton.setMaximumSize(buttonSize);
        adminButton.setMaximumSize(buttonSize);

        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align buttons
        aiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminButton.setAlignmentX(Component.CENTER_ALIGNMENT);


        leftPanel.add(menuButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add vertical spacing
        leftPanel.add(aiButton);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add vertical spacing

        // Add Admin button only if the user is admin
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            adminButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            leftPanel.add(adminButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add vertical spacing
            System.out.println("Admin user logged in, showing Admin button.");
        } else {
            System.out.println("Regular user logged in, hiding Admin button.");
        }


        // Add action listeners
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table"); // Switch to table view
            // Reload the default view (income only) when switching back to Menu
            loadCSVDataForCurrentUser("收入"); // Or "" for all data, depending on desired behavior
            // Let's keep it consistent with initial load: show income only.
        });

        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "AI"); // Switch to AI view
            // Optional: Clear previous AI results or load default prompts here
        });

        // Add listener for Admin button (only visible for admin)
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            adminButton.addActionListener(e -> {
                cardLayout.show(rightPanel, "AdminStats"); // Switch to Admin view
                // When switching to Admin view, immediately load and display stats
                displaySummaryStatistics();
            });
        }


        // Add some vertical glue to push buttons to the top
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    // Method to set up the right panel, adding different views
    private void setupRightPanel() {
        this.cardLayout = new CardLayout();
        this.rightPanel = new JPanel(this.cardLayout);

        // Create and add different panels (views)
        JPanel tablePanel = createTablePanel(); // Table view
        JPanel aiPanel = createAIPanel(); // AI view
        JPanel adminStatsPanel = createAdminStatsPanel(); // Admin stats view

        rightPanel.add(tablePanel, "Table");
        rightPanel.add(aiPanel, "AI");
        // Only add admin panel if the user is admin (although CardLayout handles non-existent cards gracefully)
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            rightPanel.add(adminStatsPanel, "AdminStats");
        }


        // Set the initially visible card (Table view)
        cardLayout.show(rightPanel, "Table");
    }

    // Method to create the table panel - same as before
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        JPanel inputPanel = createInputPanel(); // This method now initializes search fields
        tablePanel.add(inputPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(this.table);
        tableScrollPane.setPreferredSize(new Dimension(1000, 250)); // Preferred size hint
        this.table.setFillsViewportHeight(true); // Make table fill the scroll pane height
        this.table.setRowHeight(30);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Set cell renderers and editors
        this.table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(this));

        this.table.getColumnModel().getColumn(12).setCellRenderer(new ButtonRenderer());
        this.table.getColumnModel().getColumn(12).setCellEditor(new ButtonEditor(this));

        // Initial data load is done in the MenuUI constructor or createMainPanel
        // loadCSVDataForCurrentUser("收入"); // Moved to createMainPanel

        return tablePanel;
    }

    // Method to create input panel - same as before, capturing references
    private JPanel createInputPanel() {
        // ... existing implementation, capture search field references ...
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        searchTransactionTimeField = new JTextField(10);
        searchTransactionTypeField = new JTextField(10);
        searchCounterpartyField = new JTextField(10);
        searchCommodityField = new JTextField(10);
        searchInOutComboBox = new JComboBox<>(new String[]{"", "收入", "支出"});
        searchPaymentMethodField = new JTextField(10);

        inputPanel.add(new JLabel("交易时间:")); inputPanel.add(searchTransactionTimeField);
        inputPanel.add(new JLabel("交易类型:")); inputPanel.add(searchTransactionTypeField);
        inputPanel.add(new JLabel("交易对方:")); inputPanel.add(searchCounterpartyField);
        inputPanel.add(new JLabel("商品:")); inputPanel.add(searchCommodityField);
        inputPanel.add(new JLabel("收/支:")); inputPanel.add(searchInOutComboBox);
        inputPanel.add(new JLabel("支付方式:")); inputPanel.add(searchPaymentMethodField);

        searchButton = new JButton("Search");
        JButton addButton = new JButton("Add");

        inputPanel.add(searchButton);
        inputPanel.add(addButton);

        searchButton.addActionListener(e -> triggerCurrentSearch());

        addButton.addActionListener(e -> showAddTransactionDialog());

        return inputPanel;
    }

    // Method to show add transaction dialog - updated for AI integration
    private void showAddTransactionDialog() {
        // ... existing implementation with GridBagLayout, AI button, Confirm/Cancel listeners ...
        JDialog addDialog = new JDialog();
        addDialog.setTitle("添加交易");
        // addDialog.setLayout(new GridLayout(12, 2)); // Replaced by GridBagLayout
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField transactionTimeField = new JTextField(15);
        JTextField transactionTypeField = new JTextField(15);
        JButton aiSuggestButton = new JButton("AI分类建议");
        JTextField counterpartyField = new JTextField(15);
        JTextField commodityField = new JTextField(15);
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"收入", "支出"});
        JTextField paymentAmountField = new JTextField(15);
        JTextField paymentMethodField = new JTextField(15);
        JTextField currentStatusField = new JTextField(15);
        JTextField orderNumberField = new JTextField(15);
        JTextField merchantNumberField = new JTextField(15);
        JTextField remarksField = new JTextField(15);

        gbc.gridx = 0; gbc.gridy = 0; dialogPanel.add(new JLabel("交易时间:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; dialogPanel.add(transactionTimeField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1; dialogPanel.add(new JLabel("交易类型:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; dialogPanel.add(transactionTypeField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0; dialogPanel.add(aiSuggestButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; dialogPanel.add(new JLabel("交易对方:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; dialogPanel.add(counterpartyField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; dialogPanel.add(new JLabel("商品:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; dialogPanel.add(commodityField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; dialogPanel.add(new JLabel("收/支:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2; dialogPanel.add(inOutComboBox, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; dialogPanel.add(new JLabel("金额(元):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2; dialogPanel.add(paymentAmountField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; dialogPanel.add(new JLabel("支付方式:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 2; dialogPanel.add(paymentMethodField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1; dialogPanel.add(new JLabel("当前状态:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.gridwidth = 2; dialogPanel.add(currentStatusField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1; dialogPanel.add(new JLabel("交易单号:"));
        gbc.gridx = 1; gbc.gridy = 8; gbc.gridwidth = 2; dialogPanel.add(orderNumberField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1; dialogPanel.add(new JLabel("商户单号:"));
        gbc.gridx = 1; gbc.gridy = 9; gbc.gridwidth = 2; dialogPanel.add(merchantNumberField, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 1; dialogPanel.add(new JLabel("备注:"));
        gbc.gridx = 1; gbc.gridy = 10; gbc.gridwidth = 2; dialogPanel.add(remarksField, gbc); gbc.gridwidth = 1;


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("确认");
        JButton cancelButton = new JButton("取消");
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; dialogPanel.add(buttonPanel, gbc);


        addDialog.add(dialogPanel, BorderLayout.CENTER);


        aiSuggestButton.addActionListener(e -> {
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

            aiSuggestButton.setEnabled(false);
            confirmButton.setEnabled(false);

            new Thread(() -> {
                String aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);

                SwingUtilities.invokeLater(() -> {
                    if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
                        transactionTypeField.setText(aiSuggestion.trim());
                    } else {
                        JOptionPane.showMessageDialog(addDialog, "AI未能提供分类建议。", "AI提示", JOptionPane.INFORMATION_MESSAGE);
                    }

                    aiSuggestButton.setEnabled(true);
                    confirmButton.setEnabled(true);
                });
            }).start();
        });


        confirmButton.addActionListener(e -> {
            double paymentAmount = safeParseDouble(paymentAmountField.getText().trim());
            String finalTransactionType = emptyIfNull(transactionTypeField.getText().trim());

            if (finalTransactionType.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "交易类型不能为空！", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Transaction newTransaction = new Transaction(
                    emptyIfNull(transactionTimeField.getText().trim()),
                    finalTransactionType,
                    emptyIfNull(counterpartyField.getText().trim()),
                    emptyIfNull(commodityField.getText().trim()),
                    (String) inOutComboBox.getSelectedItem(),
                    paymentAmount,
                    emptyIfNull(paymentMethodField.getText().trim()),
                    emptyIfNull(currentStatusField.getText().trim()),
                    emptyIfNull(orderNumberField.getText().trim()),
                    emptyIfNull(merchantNumberField.getText().trim()),
                    emptyIfNull(remarksField.getText().trim())
            );

            if (newTransaction.getOrderNumber().isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "交易单号不能为空！", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                transactionService.addTransaction(newTransaction);

                loadCSVDataForCurrentUser(""); // Load all data after adding

                clearSearchFields();


                addDialog.dispose();
                JOptionPane.showMessageDialog(null, "交易添加成功！", "提示", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "交易添加失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> addDialog.dispose());

        addDialog.pack(); // Adjust size based on content
        addDialog.setLocationRelativeTo(this); // Center relative to the MenuUI panel
        addDialog.setVisible(true);
    }


    // Method to edit row - updated for AI integration and getting data from table model
    public void editRow(int rowIndex) {
        System.out.println("编辑行: " + rowIndex + " for user " + currentUser.getUsername());
        if (rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            Vector<String> rowData = new Vector<>();
            for (int i = 0; i <= 10; i++) { // Columns 0 to 10 are Transaction fields
                Object value = this.tableModel.getValueAt(rowIndex, i);
                rowData.add(value != null ? value.toString() : "");
            }
            System.out.println("Retrieved row data from table model for editing: " + rowData);

            String originalOrderNumber = rowData.get(8).trim();
            if (originalOrderNumber.isEmpty()) {
                JOptionPane.showMessageDialog(null, "无法编辑：交易单号为空！", "错误", JOptionPane.ERROR_MESSAGE);
                System.err.println("Attempted to edit row " + rowIndex + " but order number is empty.");
                return;
            }


            JDialog editDialog = new JDialog();
            editDialog.setTitle("修改交易信息 (订单号: " + originalOrderNumber + ")");
            editDialog.setModal(true);

            JPanel dialogPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            JTextField[] fields = new JTextField[11];
            String[] fieldNames = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};
            JButton aiSuggestButton = new JButton("AI分类建议");


            for (int i = 0; i < fieldNames.length; i++) {
                gbc.gridx = 0; gbc.gridy = i; gbc.gridwidth = 1; gbc.weightx = 0.0; dialogPanel.add(new JLabel(fieldNames[i] + ":"), gbc);
                fields[i] = new JTextField(rowData.get(i));
                if (i == 1) { // Transaction Type field
                    gbc.gridx = 1; gbc.gridy = i; gbc.weightx = 1.0; dialogPanel.add(fields[i], gbc);
                    gbc.gridx = 2; gbc.gridy = i; gbc.weightx = 0.0; dialogPanel.add(aiSuggestButton, gbc);
                    gbc.gridwidth = 1;
                } else {
                    gbc.gridx = 1; gbc.gridy = i; gbc.gridwidth = 2; gbc.weightx = 1.0; dialogPanel.add(fields[i], gbc);
                    gbc.gridwidth = 1;
                }
            }

            fields[8].setEditable(false); // Disable editing OrderNumber field


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton confirmButton = new JButton("确认");
            JButton cancelButton = new JButton("取消");
            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0; gbc.gridy = fieldNames.length; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; dialogPanel.add(buttonPanel, gbc);


            editDialog.add(dialogPanel, BorderLayout.CENTER);


            aiSuggestButton.addActionListener(e -> {
                Transaction tempTransaction = new Transaction(
                        fields[0].getText().trim(),
                        fields[1].getText().trim(),
                        fields[2].getText().trim(),
                        fields[3].getText().trim(),
                        fields[4].getText().trim(),
                        safeParseDouble(fields[5].getText().trim()),
                        fields[6].getText().trim(),
                        fields[7].getText().trim(),
                        fields[8].getText().trim(),
                        fields[9].getText().trim(),
                        fields[10].getText().trim()
                );

                aiSuggestButton.setEnabled(false);
                confirmButton.setEnabled(false);

                new Thread(() -> {
                    String aiSuggestion = collegeStudentNeeds.RecognizeTransaction(tempTransaction);

                    SwingUtilities.invokeLater(() -> {
                        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
                            fields[1].setText(aiSuggestion.trim());
                        } else {
                            JOptionPane.showMessageDialog(editDialog, "AI未能提供分类建议。", "AI提示", JOptionPane.INFORMATION_MESSAGE);
                        }

                        aiSuggestButton.setEnabled(true);
                        confirmButton.setEnabled(true);
                    });
                }).start();
            });


            confirmButton.addActionListener(e -> {
                double paymentAmount = safeParseDouble(fields[5].getText().trim());
                String finalTransactionType = emptyIfNull(fields[1].getText().trim());

                if (finalTransactionType.isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog, "交易类型不能为空！", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }


                Transaction updatedTransaction = new Transaction(
                        fields[0].getText().trim(),
                        finalTransactionType,
                        fields[2].getText().trim(),
                        fields[3].getText().trim(),
                        fields[4].getText().trim(),
                        paymentAmount,
                        fields[6].getText().trim(),
                        fields[7].getText().trim(),
                        originalOrderNumber,
                        fields[9].getText().trim(),
                        fields[10].getText().trim()
                );


                try {
                    transactionService.changeTransaction(updatedTransaction);

                    // After successful edit, update search fields and trigger search
                    System.out.println("Edit successful. Preparing to refresh display filtered by InOut: " + updatedTransaction.getInOut());
                    clearSearchFields();
                    // Safely set the combo box item, add if not present (unlikely for income/expense)
                    String updatedInOut = updatedTransaction.getInOut();
                    boolean foundInOut = false;
                    for(int i=0; i < searchInOutComboBox.getItemCount(); i++) {
                        if (updatedInOut != null && updatedInOut.equals(searchInOutComboBox.getItemAt(i))) {
                            searchInOutComboBox.setSelectedItem(updatedInOut);
                            foundInOut = true;
                            break;
                        }
                    }
                    if (!foundInOut) {
                        // Fallback: maybe clear the filter or set to default?
                        searchInOutComboBox.setSelectedItem(""); // Set to empty
                    }

                    triggerCurrentSearch();


                    editDialog.dispose();
                    JOptionPane.showMessageDialog(null, "修改成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Edited row " + rowIndex + " for order number " + originalOrderNumber + " and refreshed display.");

                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(editDialog, "修改失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(editDialog, "修改失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error during editing for order number " + originalOrderNumber);
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

    // Method to create the AI panel (Implement the placeholder)
    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField userRequestField = new JTextField(40);
        aiStartTimeField = new JTextField(10);
        aiEndTimeField = new JTextField(10);
        aiAnalyzeButton = new JButton("进行AI分析");

        inputPanel.add(new JLabel("分析要求:"));
        inputPanel.add(userRequestField);
        inputPanel.add(new JLabel("开始时间 (yyyy/MM/dd HH:mm):"));
        inputPanel.add(aiStartTimeField);
        inputPanel.add(new JLabel("结束时间 (yyyy/MM/dd HH:mm):"));
        inputPanel.add(aiEndTimeField);
        inputPanel.add(aiAnalyzeButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        aiResultArea = new JTextArea();
        aiResultArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        aiResultArea.setLineWrap(true);
        aiResultArea.setWrapStyleWord(true);
        aiResultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(aiResultArea);
        panel.add(resultScrollPane, BorderLayout.CENTER);

        aiAnalyzeButton.addActionListener(e -> {
            String userRequest = userRequestField.getText().trim();
            String startTimeStr = aiStartTimeField.getText().trim();
            String endTimeStr = aiEndTimeField.getText().trim();

            if (userRequest.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入AI分析要求。", "输入提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (startTimeStr.isEmpty() && endTimeStr.isEmpty()) { // Allow both empty to analyze all data
                // Or require at least one date: JOptionPane.showMessageDialog(this, "请输入分析的时间范围。", "输入提示", JOptionPane.INFORMATION_MESSAGE); return;
            }


            aiResultArea.setText("正在进行AI分析，请稍候...\n");
            aiAnalyzeButton.setEnabled(false);
            if(aiBudgetButton != null) aiBudgetButton.setEnabled(false);
            if(aiTipsButton != null) aiTipsButton.setEnabled(false);


            new Thread(() -> {
                // Use the injected AI service instance
                String result = aiTransactionService.analyzeTransactions(userRequest, currentUser.getTransactionFilePath(), startTimeStr, endTimeStr);

                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText(result);
                    aiAnalyzeButton.setEnabled(true);
                    if(aiBudgetButton != null) aiBudgetButton.setEnabled(true);
                    if(aiTipsButton != null) aiTipsButton.setEnabled(true);
                });
            }).start();
        });

        JPanel csButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        aiBudgetButton = new JButton("预算建议"); // Assign to instance field
        aiTipsButton = new JButton("省钱技巧"); // Assign to instance field
        csButtonsPanel.add(aiBudgetButton);
        csButtonsPanel.add(aiTipsButton);


        aiBudgetButton.addActionListener(e -> {
            aiResultArea.setText("正在生成预算建议，请稍候...\n");
            aiBudgetButton.setEnabled(false);
            aiAnalyzeButton.setEnabled(false);
            aiTipsButton.setEnabled(false);


            new Thread(() -> {
                String resultMessage;
                try {
                    // Use the injected CollegeStudentNeeds instance
                    double[] budgetRange = collegeStudentNeeds.generateBudget(currentUser.getTransactionFilePath());
                    if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] != -1) {
                        resultMessage = String.format("根据您的消费记录，下周的建议预算范围是: [%.2f元, %.2f元]", budgetRange[0], budgetRange[1]);
                    } else if (budgetRange != null && budgetRange.length == 2 && budgetRange[0] == -1) {
                        resultMessage = "暂无足够的消费记录来计算周预算建议。";
                    }
                    else {
                        resultMessage = "生成预算建议失败。";
                    }
                } catch (Exception ex) {
                    resultMessage = "生成预算建议失败！\n" + ex.getMessage();
                    ex.printStackTrace();
                }

                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText(finalResultMessage);
                    aiBudgetButton.setEnabled(true);
                    aiAnalyzeButton.setEnabled(true);
                    aiTipsButton.setEnabled(true);
                });
            }).start();
        });

        aiTipsButton.addActionListener(e -> {
            aiResultArea.setText("正在生成省钱技巧，请稍候...\n");
            aiTipsButton.setEnabled(false);
            aiAnalyzeButton.setEnabled(false);
            aiBudgetButton.setEnabled(false);


            new Thread(() -> {
                String resultMessage;
                try {
                    // Use the injected CollegeStudentNeeds instance
                    resultMessage = collegeStudentNeeds.generateTipsForSaving();
                } catch (Exception ex) {
                    resultMessage = "生成省钱技巧失败！\n" + ex.getMessage();
                    ex.printStackTrace();
                }

                String finalResultMessage = resultMessage;
                SwingUtilities.invokeLater(() -> {
                    aiResultArea.setText(finalResultMessage);
                    aiTipsButton.setEnabled(true);
                    aiAnalyzeButton.setEnabled(true);
                    aiBudgetButton.setEnabled(true);
                });
            }).start();
        });


        JPanel csPanelWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        csPanelWrapper.add(csButtonsPanel);
        panel.add(csPanelWrapper, BorderLayout.SOUTH);


        return panel;
    }


    // Method to create the Admin Stats panel (Implement the placeholder)
    private JPanel createAdminStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateStatsButton = new JButton("生成/更新统计数据"); // Assign to instance field
        refreshDisplayButton = new JButton("刷新显示"); // Assign to instance field

        controlPanel.add(generateStatsButton);
        controlPanel.add(refreshDisplayButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        adminStatsArea = new JTextArea(); // Assign to instance field
        adminStatsArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        adminStatsArea.setEditable(false);
        adminStatsArea.setLineWrap(true); // Enable line wrapping
        adminStatsArea.setWrapStyleWord(true); // Wrap at word boundaries
        JScrollPane scrollPane = new JScrollPane(adminStatsArea);
        panel.add(scrollPane, BorderLayout.CENTER);


        generateStatsButton.addActionListener(e -> {
            adminStatsArea.setText("正在生成/更新汇总统计数据，请稍候...\n");
            generateStatsButton.setEnabled(false);
            refreshDisplayButton.setEnabled(false);

            new Thread(() -> {
                String message;
                try {
                    // Use the injected SummaryStatisticService
                    summaryStatisticService.generateAndSaveWeeklyStatistics();
                    message = "汇总统计数据生成/更新成功！\n请点击 '刷新显示' 查看最新数据。";
                    System.out.println(message);
                } catch (Exception ex) {
                    message = "汇总统计数据生成/更新失败！\n" + ex.getMessage();
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

        // Initial display when the panel is first shown (Optional)
        // Load existing stats when the panel is created, before it's potentially shown
        // This avoids a blank screen initially.
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> adminStatsArea.setText("正在加载现有统计数据...\n"));
            try {
                List<SummaryStatistic> initialStats = summaryStatisticService.getAllSummaryStatistics();
                if (!initialStats.isEmpty()) {
                    // If initial stats exist, display them
                    SwingUtilities.invokeLater(this::displaySummaryStatistics);
                } else {
                    SwingUtilities.invokeLater(() -> adminStatsArea.setText("没有找到现有的汇总统计数据。\n请点击 '生成/更新统计数据' 按钮来生成。"));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> adminStatsArea.setText("加载现有统计数据失败！\n" + ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();


        return panel;
    }

    // Method to display summary statistics (Implement the placeholder)
    private void displaySummaryStatistics() {
        adminStatsArea.setText("正在加载汇总统计数据...\n");
        // Optional: Disable buttons while loading
        if(generateStatsButton != null) generateStatsButton.setEnabled(false);
        if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(false);


        new Thread(() -> {
            String displayContent;
            try {
                List<SummaryStatistic> stats = summaryStatisticService.getAllSummaryStatistics();
                if (stats.isEmpty()) {
                    displayContent = "目前没有汇总统计数据。\n请先点击 '生成/更新统计数据' 按钮。";
                } else {
                    StringBuilder sb = new StringBuilder("===== 汇总统计数据 =====\n\n");
                    // Sort stats by week identifier (chronologically) before displaying
                    stats.sort(Comparator.comparing(SummaryStatistic::getWeekIdentifier));

                    // Display newest week first
                    for (int i = stats.size() - 1; i >= 0; i--) {
                        SummaryStatistic stat = stats.get(i);
                        sb.append("周标识: ").append(stat.getWeekIdentifier()).append("\n");
                        sb.append("  总收入: ").append(String.format("%.2f", stat.getTotalIncomeAllUsers())).append("元\n");
                        sb.append("  总支出: ").append(String.format("%.2f", stat.getTotalExpenseAllUsers())).append("元\n");
                        // Only show top category if there was actual expense in that category
                        if (stat.getTopExpenseCategoryAmount() > 0) {
                            sb.append("  最高支出类别: ").append(stat.getTopExpenseCategory()).append(" (").append(String.format("%.2f", stat.getTopExpenseCategoryAmount())).append("元)\n");
                        } else {
                            sb.append("  最高支出类别: 无显著支出类别\n"); // Or "无支出" if total expense is 0
                        }

                        sb.append("  参与用户数: ").append(stat.getNumberOfUsersWithTransactions()).append("\n");
                        sb.append("  生成时间: ").append(stat.getTimestampGenerated()).append("\n");
                        sb.append("--------------------\n");
                    }
                    displayContent = sb.toString();
                }
            } catch (IOException ex) {
                displayContent = "加载汇总统计数据失败！\n" + ex.getMessage();
                ex.printStackTrace();
            }

            String finalDisplayContent = displayContent;
            SwingUtilities.invokeLater(() -> {
                adminStatsArea.setText(finalDisplayContent);
                // Re-enable buttons
                if(generateStatsButton != null) generateStatsButton.setEnabled(true);
                if(refreshDisplayButton != null) refreshDisplayButton.setEnabled(true);
            });
        }).start();
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

            // Optional: Ask for confirmation before deleting
            int confirm = JOptionPane.showConfirmDialog(
                    this, // Parent component
                    "确定要删除订单号为 '" + orderNumber + "' 的交易吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // Use the injected transactionService instance
                    boolean deleted = transactionService.deleteTransaction(orderNumber);

                    if (deleted) {
                        // Data is removed from CSV and cache invalidated by service.
                        // Update the UI model directly by removing the row.
                        // This is faster than reloading all data for just one deletion.
                        this.tableModel.removeRow(rowIndex);

                        JOptionPane.showMessageDialog(null, "删除成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        System.out.println("Deleted row " + rowIndex + " with order number " + orderNumber + " from UI.");

                        // After delete, refresh the view by re-applying the current search/filter criteria.
                        triggerCurrentSearch();

                    } else {
                        // This case means the service said it wasn't deleted (likely not found)
                        // This might happen if the underlying data changed between loading and deleting
                        JOptionPane.showMessageDialog(null, "删除失败：未找到对应的交易单号 " + orderNumber, "错误", JOptionPane.ERROR_MESSAGE);
                        System.err.println("Delete failed: order number " + orderNumber + " not found by service.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "删除失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error during deletion for order number " + orderNumber);
                }
            } else {
                System.out.println("Deletion cancelled by user for order number: " + orderNumber);
            }
        } else {
            System.err.println("Attempted to delete row with invalid index: " + rowIndex + ". Table row count: " + this.tableModel.getRowCount());
        }
    }

    // ... rest of helper methods (createRowFromTransaction, searchData, deleteRow, editRow, safeParseDouble, clearSearchFields, triggerCurrentSearch, emptyIfNull, getTable) ...

    // Method to create table row from Transaction object - no changes needed here
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

    // Method to search data - same as before
    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) {
        // ... existing implementation ...
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
            JOptionPane.showMessageDialog(null, "查询失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }


    // ... safeParseDouble, clearSearchFields, triggerCurrentSearch, emptyIfNull, getTable methods ...
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