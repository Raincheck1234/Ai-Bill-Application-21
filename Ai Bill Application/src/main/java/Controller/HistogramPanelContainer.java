package Controller;
import Service.AIservice.AITransactionService;
import Service.TransactionService;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistogramPanelContainer extends JPanel {
    private HistogramPanel histogramPanel;
    private JTextArea textArea;
    private JSplitPane splitPane;
    private boolean isHistogramVisible = true;
    private boolean isTextVisible = true;

    String userRequest = "分析账单";
    String filePath = "data/transactions.csv";
    String startTime = "2024-01-01";
    String endTime = "2024-12-31";

    private String aiResultText = ""; // 存储 AI 分析结果

    public HistogramPanelContainer() {
        setLayout(new BorderLayout(10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnShowHistogram = new JButton("显示直方图");
        JButton btnShowText1 = new JButton("显示文本 1");
        JButton btnShowText2 = new JButton("显示文本 2");

        buttonPanel.add(btnShowHistogram);
        buttonPanel.add(btnShowText1);
        buttonPanel.add(btnShowText2);
        add(buttonPanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        JScrollPane textScrollPane = new JScrollPane(textArea);

        histogramPanel = new HistogramPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, histogramPanel);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // 按钮点击监听器
        btnShowHistogram.addActionListener(e -> toggleHistogram());
        btnShowText1.addActionListener(e -> {
            // 调用后台分析并显示结果
            analyzeTransactionsInBackground(userRequest, filePath, startTime, endTime);
        });
        btnShowText2.addActionListener(e -> {
            // 你可以根据需求设置其他的文本显示
            analyzeTransactionsInBackground(userRequest, filePath, startTime, endTime);
        });
    }

    // 切换直方图的显示和隐藏
    private void toggleHistogram() {
        if (isHistogramVisible) {
            histogramPanel.setVisible(false);
        } else {
            showHistogram();
            histogramPanel.setVisible(true);
        }
        isHistogramVisible = !isHistogramVisible;
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(isHistogramVisible ? 0.5 : 0.0));
    }



    // 显示直方图的函数，模拟数据
    private void showHistogram() {
        int[] data = DataGenerator.generateData(1000, 100);
        Histogram histogram = new Histogram(data, 10);
        histogramPanel.updateData(histogram.computeFrequency());
    }

    // 设置 AI 结果文本（供 AI 结果处理后调用）
    public void setAiResultText(String result) {
        this.aiResultText = result;
    }

    // 从后台线程调用，获取 AI 分析结果并更新界面
    public void analyzeTransactionsInBackground(String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            System.out.println("分析任务开始...");  // 调试输出
            String result = new AITransactionService().analyzeTransactions(userRequest, filePath, startTimeStr, endTimeStr);

            // 输出结果查看是否有返回
            System.out.println("AI分析结果: " + result);

            if (result != null && !result.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    setAiResultText(result);  // 更新 AI 结果文本
                    toggleText(result);       // 显示 AI 结果
                });
            } else {
                System.out.println("AI分析没有返回结果!");
            }
        });
        executor.shutdown();
    }

    public void toggleText(String text) {
        System.out.println("切换文本显示...");  // 调试输出
        if (isTextVisible) {
            textArea.setText("");  // 清空文本区域
            textArea.setVisible(false);
        } else {
            aiResultText = text;
            textArea.setText(text);
            textArea.setVisible(true);
        }
        isTextVisible = !isTextVisible;
        SwingUtilities.invokeLater(() -> {
            System.out.println("更新分割条位置...");  // 调试输出
            splitPane.setDividerLocation(isTextVisible ? 0.5 : 0.0);
        });
    }

}

