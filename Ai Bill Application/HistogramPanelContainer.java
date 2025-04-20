package Controller;

import javax.swing.*;
import java.awt.*;

// 直方图面板容器
public class HistogramPanelContainer extends JPanel {
    private HistogramPanel histogramPanel;
    private JTextArea textArea;
    private JSplitPane splitPane;
    private boolean isHistogramVisible = true;
    private boolean isTextVisible = true;

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

        btnShowHistogram.addActionListener(e -> toggleHistogram());
        btnShowText1.addActionListener(e -> toggleText("这是一段非常长的文本..."));
        btnShowText2.addActionListener(e -> toggleText("222222222222222"));
    }

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

    private void toggleText(String text) {
        if (isTextVisible) {
            textArea.setText("");
        } else {
            textArea.setText(text);
        }
        isTextVisible = !isTextVisible;
    }



    private void showHistogram() {
        int[] data = DataGenerator.generateData(1000, 100);
        Histogram histogram = new Histogram(data, 10);
        histogramPanel.updateData(histogram.computeFrequency());
    }
}
