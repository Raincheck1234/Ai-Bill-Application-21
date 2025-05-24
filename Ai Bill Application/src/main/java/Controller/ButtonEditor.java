package Controller;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final JPanel panel = new JPanel(new BorderLayout()); // Use BorderLayout
    private final JButton button = new JButton();
    private int rowIndex;
    private final MenuUI menuUI;

    public ButtonEditor(MenuUI menuUI) {
        this.menuUI = menuUI;

        // Set button style
        button.setFocusPainted(false); // Remove focus border from button
        button.setPreferredSize(new Dimension(80, 30)); // Set fixed size for the button

        // Add button to panel
        panel.add(button, BorderLayout.CENTER);

        // Add action listener to the button
        button.addActionListener(e -> {
            System.out.println("Button click event triggered: " + button.getText()); // "按钮点击事件触发: "
            fireEditingStopped(); // Stop editing
            String buttonText = button.getText();
            if ("Modify".equals(buttonText)) {
                menuUI.editRow(rowIndex); // Call MenuUI's editRow method
            } else if ("Delete".equals(buttonText)) {
                int confirm = JOptionPane.showConfirmDialog(panel, "Are you sure you want to delete this row?", "Confirm Delete", JOptionPane.YES_NO_OPTION); // "Are you sure you want to delete this row?", "Confirm deletion"
                if (confirm == JOptionPane.YES_OPTION) {
                    menuUI.deleteRow(rowIndex); // Call MenuUI's deleteRow method
                }
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.rowIndex = row; // Update current row index
        button.setText(value != null ? value.toString() : ""); // Set button text based on cell value
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return button.getText(); // Return the current text of the button
    }
}