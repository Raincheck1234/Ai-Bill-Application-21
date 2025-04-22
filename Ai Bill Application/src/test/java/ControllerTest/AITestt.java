package ControllerTest;

import Controller.HistogramPanelContainer;

import javax.swing.*;

public class AITestt {
    public static void main(String[] args) {
        // 设置外观为系统默认
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 创建窗口
        JFrame frame = new JFrame("Histogram Panel 测试");
        HistogramPanelContainer container = new HistogramPanelContainer();

        // 使用 SwingUtilities.invokeAndWait 来确保 UI 线程安全
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 600);
            frame.setContentPane(container);
            frame.setVisible(true);

            // 模拟调用 AI 结果（这里你可以改成真实的文件路径和参数）
            container.analyzeTransactionsInBackground(
                    "请分析交易中的异常模式",
                    "data/transactions.csv",  // 确保路径存在或替换为你实际的测试路径
                    "2022-01-01",
                    "2023-01-01"
            );
        });

        // 你可以适当的加入等待时间来保证后台线程有足够的时间完成
        try {
            Thread.sleep(3000);  // 等待 3 秒钟，以确保后台任务执行完毕（根据实际情况调整时间）
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
