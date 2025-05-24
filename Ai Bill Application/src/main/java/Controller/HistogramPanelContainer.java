package Controller;

import javax.swing.*;
import java.awt.*;

// Histogram Panel Container
public class HistogramPanelContainer extends JPanel {
    private HistogramPanel histogramPanel;
    private JTextArea textArea;
    private JSplitPane splitPane;
    private boolean isHistogramVisible = true;
    private boolean isTextVisible = true;

    public HistogramPanelContainer() {
        setLayout(new BorderLayout(10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnShowHistogram = new JButton("Show Histogram");
        JButton btnShowText1 = new JButton("Show Text 1");
        JButton btnShowText2 = new JButton("Show Text 2");

        buttonPanel.add(btnShowHistogram);
        buttonPanel.add(btnShowText1);
        buttonPanel.add(btnShowText2);
        add(buttonPanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18)); // "微软雅黑" (Microsoft YaHei is a common font name)
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        JScrollPane textScrollPane = new JScrollPane(textArea);

        histogramPanel = new HistogramPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, histogramPanel);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        btnShowHistogram.addActionListener(e -> toggleHistogram());
        btnShowText1.addActionListener(e -> toggleText("This is a very long text..."));
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