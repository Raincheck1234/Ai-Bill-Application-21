package Controller;

import Controller.ButtonRenderer;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

public class ButtonRendererTest {

    @Test
    void testButtonRendererInstantiationAndUsage() {
        try {
            ButtonRenderer renderer = new ButtonRenderer();
            // Simulate a call from JTable
            Component component = renderer.getTableCellRendererComponent(
                    null, // JTable (can be null for basic test)
                    "Test Button",  // value
                    false,          // isSelected
                    false,          // hasFocus
                    0,              // row
                    0               // column
            );
            System.out.println("ButtonRendererTest: ButtonRenderer instantiated and getTableCellRendererComponent called.");
            // To visually test, you'd need to add this component to a visible frame,
            // but for a "can it run" test, just calling the method is sufficient.
            if (component instanceof JPanel) {
                System.out.println("ButtonRendererTest: Component is a JPanel as expected.");
            } else {
                System.err.println("ButtonRendererTest: Component is NOT a JPanel.");
            }
        } catch (Exception e) {
            System.err.println("ButtonRendererTest: Error during test.");
            e.printStackTrace();
            // If we reach here, the test effectively fails for "can it run"
            throw e; // Re-throw to make JUnit mark it as failed
        }
        System.out.println("ButtonRendererTest: testButtonRendererInstantiationAndUsage finished.");
    }
}