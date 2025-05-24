package Controller;

import Controller.HistogramPanelContainer;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

public class HistogramPanelContainerTest {

    @Test
    void testDisplayHistogramPanelContainer() {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("HistogramPanelContainer Test Display");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(800, 600);
                frame.add(new HistogramPanelContainer());
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println("HistogramPanelContainerTest: HistogramPanelContainer displayed.");
            } catch (Exception e) {
                System.err.println("HistogramPanelContainerTest: Error creating or displaying HistogramPanelContainer.");
                e.printStackTrace();
            }
        });

        try {
            Thread.sleep(5000); // Keep frame visible for 5 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("HistogramPanelContainerTest: testDisplayHistogramPanelContainer finished.");
    }
}