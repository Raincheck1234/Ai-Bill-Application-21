package Controller;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

class ButtonRenderer extends DefaultTableCellRenderer {
    private final JPanel panel = new JPanel(new BorderLayout()); // Use BorderLayout
    private final JButton button = new JButton();

    public ButtonRenderer() {
        button.setFocusPainted(false); // Remove focus border from button
        button.setPreferredSize(new Dimension(80, 30)); // Set fixed size for the button
        panel.add(button, BorderLayout.CENTER); // Add button to the panel center
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Set button text based on cell value
        button.setText(value != null ? value.toString() : "");
        return panel;
    }
}