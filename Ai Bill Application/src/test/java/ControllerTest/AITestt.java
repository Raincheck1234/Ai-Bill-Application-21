package ControllerTest;

import Controller.HistogramPanelContainer;

import javax.swing.*;

public class AITestt {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Histogram Panel 测试");
        HistogramPanelContainer container = new HistogramPanelContainer();

        // 添加 AI 结果监听器
        container.setAIResultListener(result -> {
            System.out.println("收到 AI 分析结果: " + result);
            // 你可以在这里做更多操作，比如保存日志、更新 UI 等
        });

        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 600);
            frame.setContentPane(container);
            frame.setLocationRelativeTo(null); // 居中显示
            frame.setVisible(true);

            container.analyzeTransactionsInBackground(
                    "请分析交易中的异常模式",
                    "data/transactions.csv",
                    "2022-01-01",
                    "2023-01-01"
            );
        });
    }
}
