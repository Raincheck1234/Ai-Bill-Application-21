package Controller;

import DAO.CsvTransactionDao;

import Service.Impl.TransactionServiceImpl;

import Service.Impl.TransactionServiceImpl;

import model.Transaction;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class MenuUI {
    private static final String FILE_PATH = "Ai Bill Application/src/main/resources/CSVForm/0001.csv";
    private static DefaultTableModel tableModel;
    private static Vector<Vector<String>> allData = new Vector<>();
    private static TransactionServiceImpl transactionService = new TransactionServiceImpl(new CsvTransactionDao());
    private JTable table;  // 把table定义为静态字段
    private HistogramPanelContainer histogramPanelContainer; // 添加 HistogramPanelContainer 实例
    private JPanel rightPanel; // 右边的面板，用于显示搜索和表格界面或 AI 界面
    private CardLayout cardLayout; // 用于管理界面切换的布局

    public MenuUI(){
        String[] columnNames = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注", "Modify", "Delete"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);

        histogramPanelContainer = new HistogramPanelContainer(); // 初始化 HistogramPanelContainer
        cardLayout = new CardLayout(); // 初始化 CardLayout
        rightPanel = new JPanel(cardLayout); // 初始化 rightPanel
    }

    public JPanel createMainPanel() {
        // 主面板，使用 BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 左边的面板，包含 Menu 和 AI 按钮
        JPanel leftPanel = createLeftPanel();
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // 右边的面板，用于显示搜索和表格界面或 AI 界面
        setupRightPanel();
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    // 创建左边的面板，包含 Menu 和 AI 按钮
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        JButton menuButton = new JButton("Menu");
        JButton aiButton = new JButton("AI");

        leftPanel.add(menuButton);
        leftPanel.add(aiButton);

        // 为 Menu 按钮添加 ActionListener
        menuButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Table"); // 切换到表格界面
        });

        // 为 AI 按钮添加 ActionListener
        aiButton.addActionListener(e -> {
            cardLayout.show(rightPanel, "Histogram"); // 切换到直方图界面
        });

        return leftPanel;
    }

    // 设置右边的面板，包含搜索和表格界面或 AI 界面
    private void setupRightPanel() {
        // 创建搜索和表格的面板
        JPanel tablePanel = createTablePanel();

        // 将表格面板和直方图面板添加到 rightPanel
        rightPanel.add(tablePanel, "Table");
        rightPanel.add(histogramPanelContainer, "Histogram");
    }

    // 创建搜索和表格的面板
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());

        // 创建输入面板
        JPanel inputPanel = createInputPanel();
        tablePanel.add(inputPanel, BorderLayout.NORTH);

        // 创建表格的滚动面板
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(1000, 250));
        table.setRowHeight(30); // 设置表格的行高

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // 为 Modify 列设置渲染器和编辑器
        table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(this));

        // 为 Delete 列设置渲染器和编辑器
        table.getColumnModel().getColumn(12).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(12).setCellEditor(new ButtonEditor(this));

        // 加载 CSV 数据
        loadCSVData(FILE_PATH);

        return tablePanel;
    }

    // 创建输入面板
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // 创建输入字段
        JTextField transactionTimeField = new JTextField(10); // 交易时间输入框
        JTextField transactionTypeField = new JTextField(10); // 交易类型输入框
        JTextField counterpartyField = new JTextField(10);    // 交易对方输入框
        JTextField commodityField = new JTextField(10);      // 商品输入框
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"收入", "支出"}); // 收/支下拉框
        JTextField paymentMethodField = new JTextField(10);  // 支付方式输入框

        // 添加标签和输入字段到输入面板
        inputPanel.add(new JLabel("交易时间:"));
        inputPanel.add(transactionTimeField);
        inputPanel.add(new JLabel("交易类型:"));
        inputPanel.add(transactionTypeField);
        inputPanel.add(new JLabel("交易对方:"));
        inputPanel.add(counterpartyField);
        inputPanel.add(new JLabel("商品:"));
        inputPanel.add(commodityField);
        inputPanel.add(new JLabel("收/支:"));
        inputPanel.add(inOutComboBox);
        inputPanel.add(new JLabel("支付方式:"));
        inputPanel.add(paymentMethodField);

        // 创建 Search 和 Add 按钮
        JButton searchButton = new JButton("Search");
        JButton addButton = new JButton("Add");

        // 将按钮添加到输入面板
        inputPanel.add(searchButton);
        inputPanel.add(addButton);

        // 为 Search 按钮添加 ActionListener
        searchButton.addActionListener(e -> {
            // 获取输入字段的值
            searchData(
                    transactionTimeField.getText().trim(),
                    transactionTypeField.getText().trim(),
                    counterpartyField.getText().trim(),
                    commodityField.getText().trim(),
                    (String) inOutComboBox.getSelectedItem(),
                    paymentMethodField.getText().trim()
            );
        });

        // 为 Add 按钮添加 ActionListener
        addButton.addActionListener(e -> {
            // 弹出对话框，输入交易信息
            showAddTransactionDialog();
        });

        return inputPanel;
    }

    // 弹出对话框，输入交易信息
    private void showAddTransactionDialog() {
        // 创建对话框
        JDialog addDialog = new JDialog();
        addDialog.setTitle("添加交易");
        addDialog.setLayout(new GridLayout(12, 2)); // 11 个字段 + 1 个按钮行

        // 创建输入字段
        JTextField transactionTimeField = new JTextField();
        JTextField transactionTypeField = new JTextField();
        JTextField counterpartyField = new JTextField();
        JTextField commodityField = new JTextField();
        JComboBox<String> inOutComboBox = new JComboBox<>(new String[]{"收入", "支出"});
        JTextField paymentAmountField = new JTextField();
        JTextField paymentMethodField = new JTextField();
        JTextField currentStatusField = new JTextField();
        JTextField orderNumberField = new JTextField();
        JTextField merchantNumberField = new JTextField();
        JTextField remarksField = new JTextField();

        // 添加标签和输入字段到对话框
        addDialog.add(new JLabel("交易时间:"));
        addDialog.add(transactionTimeField);
        addDialog.add(new JLabel("交易类型:"));
        addDialog.add(transactionTypeField);
        addDialog.add(new JLabel("交易对方:"));
        addDialog.add(counterpartyField);
        addDialog.add(new JLabel("商品:"));
        addDialog.add(commodityField);
        addDialog.add(new JLabel("收/支:"));
        addDialog.add(inOutComboBox);
        addDialog.add(new JLabel("金额(元):"));
        addDialog.add(paymentAmountField);
        addDialog.add(new JLabel("支付方式:"));
        addDialog.add(paymentMethodField);
        addDialog.add(new JLabel("当前状态:"));
        addDialog.add(currentStatusField);
        addDialog.add(new JLabel("交易单号:"));
        addDialog.add(orderNumberField);
        addDialog.add(new JLabel("商户单号:"));
        addDialog.add(merchantNumberField);
        addDialog.add(new JLabel("备注:"));
        addDialog.add(remarksField);

        // 添加确认按钮
        JButton confirmButton = new JButton("确认");
        confirmButton.addActionListener(e -> {
            // 获取输入字段的值，若为空则设置为默认值
            String transactionTime = emptyIfNull(transactionTimeField.getText().trim());
            String transactionType = emptyIfNull(transactionTypeField.getText().trim());
            String counterparty = emptyIfNull(counterpartyField.getText().trim());
            String commodity = emptyIfNull(commodityField.getText().trim());
            String inOut = (String) inOutComboBox.getSelectedItem();
            String paymentAmountText = paymentAmountField.getText().trim();
            double paymentAmount = paymentAmountText.isEmpty() ? 0.0 : Double.parseDouble(paymentAmountText); // 处理空字符串
            String paymentMethod = emptyIfNull(paymentMethodField.getText().trim());
            String currentStatus = emptyIfNull(currentStatusField.getText().trim());
            String orderNumber = emptyIfNull(orderNumberField.getText().trim());
            String merchantNumber = emptyIfNull(merchantNumberField.getText().trim());
            String remarks = emptyIfNull(remarksField.getText().trim());

            // 创建 Transaction 对象
            Transaction newTransaction = new Transaction(
                    transactionTime,
                    transactionType,
                    counterparty,
                    commodity,
                    inOut,
                    paymentAmount,
                    paymentMethod,
                    currentStatus,
                    orderNumber,
                    merchantNumber,
                    remarks
            );

            try {
                // 调用 TransactionServiceImpl 的 addTransaction 方法
                transactionService.addTransaction(newTransaction);

                // 重新加载 CSV 数据以更新表格
                loadCSVData(FILE_PATH);

                // 关闭对话框
                addDialog.dispose();

                JOptionPane.showMessageDialog(null, "交易添加成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "交易添加失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        addDialog.add(confirmButton);

        // 设置对话框大小并显示
        addDialog.setSize(400, 300);
        addDialog.setModal(true); // 设置为模态对话框
        addDialog.setVisible(true);
    }

    // 加载 CSV 数据
    public void loadCSVData(String filePath) {
        allData.clear();
        tableModel.setRowCount(0);

        CsvTransactionDao csvTransactionDao = new CsvTransactionDao();

        try {
            List<Transaction> transactions = csvTransactionDao.loadFromCSV(filePath);
            for (Transaction transaction : transactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                allData.add(row);
                tableModel.addRow(row);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "CSV文件读取失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 搜索数据
    public void searchData(String query1, String query2, String query3, String query4, String query6, String query5) {
        tableModel.setRowCount(0);
        Transaction searchCriteria = new Transaction(query1, query2, query3, query4, query6, 0, query5, "", "", "", "");

        try {
            List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);
            for (Transaction transaction : transactions) {
                Vector<String> row = createRowFromTransaction(transaction);
                tableModel.addRow(row);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "查询失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 从 Transaction 对象创建表格行
    private Vector<String> createRowFromTransaction(Transaction transaction) {
        Vector<String> row = new Vector<>();
        row.add(transaction.getTransactionTime());
        row.add(transaction.getTransactionType());
        row.add(transaction.getCounterparty());
        row.add(transaction.getCommodity());
        row.add(transaction.getInOut());
        row.add(String.valueOf(transaction.getPaymentAmount()));
        row.add(transaction.getPaymentMethod());
        row.add(transaction.getCurrentStatus());
        row.add(transaction.getOrderNumber());
        row.add(transaction.getMerchantNumber());
        row.add(transaction.getRemarks());
        row.add("Modify"); // Modify 按钮
        row.add("Delete"); // 添加 Delete 按钮
        return row;
    }

    // 删除行
    public void deleteRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < allData.size()) {
            String orderNumber = allData.get(rowIndex).get(8).trim();
            System.out.println("尝试删除的交易单号: " + orderNumber); // 打印交易单号

            try {
                if (transactionService.deleteTransaction(orderNumber)) {
                    allData.remove(rowIndex);
                    tableModel.removeRow(rowIndex);
                    JOptionPane.showMessageDialog(null, "删除成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "删除失败：未找到对应的交易单号", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "删除失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 编辑行
    public void editRow(int rowIndex) {
        System.out.println("编辑行: " + rowIndex);
        if (rowIndex >= 0 && rowIndex < allData.size()) {
            Vector<String> rowData = allData.get(rowIndex);

            // 创建一个面板用于显示编辑字段
            JPanel panel = new JPanel(new GridLayout(rowData.size() - 2, 2)); // 排除最后两列

            // 创建字段数组，用于存储用户输入的值
            JTextField[] fields = new JTextField[rowData.size() - 2]; // 排除最后两列

            // 遍历 rowData，跳过最后两列（"Modify" 和 "Delete"）
            for (int i = 0; i < rowData.size() - 2; i++) {
                panel.add(new JLabel(tableModel.getColumnName(i))); // 添加列名标签
                fields[i] = new JTextField(rowData.get(i)); // 添加输入字段
                panel.add(fields[i]);
            }

            // 弹出对话框，让用户修改数据
            int result = JOptionPane.showConfirmDialog(null, panel, "修改交易信息", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                // 用户点击了确认按钮，更新交易信息
                Transaction transaction = new Transaction(
                        fields[0].getText().trim(), // 交易时间
                        fields[1].getText().trim(), // 交易类型
                        fields[2].getText().trim(), // 交易对方
                        fields[3].getText().trim(), // 商品
                        fields[4].getText().trim(), // 收/支
                        Double.parseDouble(fields[5].getText().trim()), // 金额(元)
                        fields[6].getText().trim(), // 支付方式
                        fields[7].getText().trim(), // 当前状态
                        fields[8].getText().trim(), // 交易单号
                        fields[9].getText().trim(), // 商户单号
                        fields[10].getText().trim() // 备注
                );

                try {
                    // 调用 TransactionServiceImpl 的 changeTransaction 方法更新交易
                    transactionService.changeTransaction(transaction);

                    // 重新加载 CSV 数据以更新表格
                    loadCSVData(FILE_PATH);

                    JOptionPane.showMessageDialog(null, "修改成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "修改失败！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * 查找对话框中的输入字段
     *
     * @param dialog 对话框
     * @param index  输入字段的索引
     * @return 输入字段
     */
    /**
     * 查找容器中的输入字段
     *
     * @param container 容器（可以是 JPanel 或 JDialog）
     * @param index     输入字段的索引
     * @return 输入字段
     */
    private JTextField findTextField(Container container, int index) {
        int count = 0;
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField) {
                if (count == index) {
                    return (JTextField) component;
                }
                count++;
            }
        }
        return null;
    }

    /**
     * 查找对话框中的下拉框
     *
     * @param dialog 对话框
     * @return 下拉框
     */
    /**
     * 查找容器中的下拉框
     *
     * @param container 容器（可以是 JPanel 或 JDialog）
     * @return 下拉框
     */
    private JComboBox<String> findComboBox(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                return (JComboBox<String>) component;
            }
        }
        return null;
    }

    /**
     * 如果字段为 null，则返回空文本
     *
     * @param value 字段值
     * @return 非 null 的字段值
     */
    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    // 添加此方法以便测试时可以获取table
    public JTable getTable() {
        return table;
    }
}