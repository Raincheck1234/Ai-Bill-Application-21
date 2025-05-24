package Controller;

import Controller.VisualizationPanel;
import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

public class VisualizationPanelTest {

    @Test
    void testDisplayVisualizationPanel() {
        // Setup a TransactionService instance
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        User testUser = userService.authenticate("user1", "pass123"); // Using user1 for transaction data

        if (testUser == null) {
            System.err.println("VisualizationPanelTest: Failed to authenticate test user 'user1'. Check credentials and users.csv.");
            return;
        }

        TransactionService transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());

        SwingUtilities.invokeLater(() -> {
            try {
                VisualizationPanel visualizationPanel = new VisualizationPanel(transactionService);
                // Optionally call a method to load initial data/charts if available
                // visualizationPanel.refreshPanelData(); // As per VisualizationPanel's own method

                JFrame frame = new JFrame("VisualizationPanel Test Display");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(800, 600);
                frame.add(visualizationPanel);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println("VisualizationPanelTest: VisualizationPanel displayed.");

                // Call refreshPanelData after the panel is visible and has a size
                SwingUtilities.invokeLater(visualizationPanel::refreshPanelData);

            } catch (Exception e) {
                System.err.println("VisualizationPanelTest: Error creating or displaying VisualizationPanel.");
                e.printStackTrace();
            }
        });

        try {
            Thread.sleep(5000); // Keep frame visible for 5 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("VisualizationPanelTest: testDisplayVisualizationPanel finished.");
    }
}