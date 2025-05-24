package Controller;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Random;

// Data Generation Class
class DataGenerator {
    public static int[] generateData(int numberOfDataPoints, int maxValue) {
        Random random = new Random();
        int[] data = new int[numberOfDataPoints];
        for (int i = 0; i < numberOfDataPoints; i++) {
            data[i] = random.nextInt(maxValue);
        }
        return data;
    }
}

// Histogram Calculation Class
class Histogram {
    private int binSize;
    private int[] data;

    public Histogram(int[] data, int binSize) {
        this.data = data;
        this.binSize = binSize;
    }

    public HashMap<Integer, Integer> computeFrequency() {
        HashMap<Integer, Integer> frequencyMap = new HashMap<>();
        for (int number : data) {
            int bin = number / binSize;
            frequencyMap.put(bin, frequencyMap.getOrDefault(bin, 0) + 1);
        }
        return frequencyMap;
    }
}

// Histogram GUI Drawing Class
class HistogramPanel extends JPanel {
    HashMap<Integer, Integer> frequencyMap;

    public HistogramPanel() {
        this.frequencyMap = new HashMap<>();
    }

    public void updateData(HashMap<Integer, Integer> frequencyMap) {
        this.frequencyMap = frequencyMap;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int barWidth = 40;
        int gap = 10;
        int maxFrequency = frequencyMap.values().stream().mapToInt(v -> v).max().orElse(1);

        int index = 0;
        for (Integer key : frequencyMap.keySet()) {
            int height = (int) ((frequencyMap.get(key) / (double) maxFrequency) * getHeight());
            g.setColor(Color.BLUE);
            g.fillRect(index * (barWidth + gap), getHeight() - height, barWidth, height);
            g.setColor(Color.BLACK);
            g.drawRect(index * (barWidth + gap), getHeight() - height, barWidth, height);
            g.drawString("Bin " + key, index * (barWidth + gap) + 5, getHeight() - 5);
            index++;
        }
    }
}


// Main Window Class
public class HistogramExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Data Analysis Interface"); // "数据分析界面"
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            // Assuming HistogramPanelContainer is defined elsewhere or if it's a typo and should be HistogramPanel
            // If HistogramPanelContainer is a separate class, its content is not provided for translation.
            // For now, I'll assume it might be a placeholder for HistogramPanel or a similar container.
            // If HistogramPanel itself is to be added:
            // HistogramPanel histogramPanel = new HistogramPanel();
            // // Example data for histogram (if not loaded through a container)
            // int[] data = DataGenerator.generateData(100, 100);
            // Histogram histogram = new Histogram(data, 10);
            // histogramPanel.updateData(histogram.computeFrequency());
            // frame.add(histogramPanel);
            // If HistogramPanelContainer is a real class, its name implies it contains HistogramPanel.
            // The user will need to provide that class if its internal strings/comments need translation.
            frame.add(new HistogramPanelContainer()); // This line remains, assuming HistogramPanelContainer exists
            frame.setVisible(true);
        });
    }
}