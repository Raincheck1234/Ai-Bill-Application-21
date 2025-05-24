package Controller;

import Controller.MenuUI;
import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.SummaryStatisticDao;
import DAO.TransactionDao;
import DAO.UserDao;
import Interceptor.Login.LoginDialog; // Not used directly for MenuUI test, but part of typical flow
import Service.AIservice.AITransactionService;
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.SummaryStatisticService;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants; // For paths

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

public class MenuUITest {

    @Test
    void testDisplayMenuUI() {
        // Setup services and user
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        // Authenticate a test user (e.g., user1)
        User testUser = userService.authenticate("user1", "pass123");
        if (testUser == null) {
            System.err.println("MenuUITest: Failed to authenticate test user 'user1'. Check credentials and users.csv.");
            // Attempt with admin if user1 fails, for robustness in test setup
            testUser = userService.authenticate("admin", "admin123");
            if (testUser == null) {
                System.err.println("MenuUITest: Failed to authenticate test user 'admin' as well. Test cannot proceed.");
                return;
            }
        }
        final User currentUser = testUser; // Effectively final for lambda

        TransactionDao transactionDao = new CsvTransactionDao();
        TransactionService transactionService = new TransactionServiceImpl(currentUser.getTransactionFilePath());

        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao();
        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao);

        AITransactionService aiTransactionService = new AITransactionService(transactionService);
        CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionService);

        SwingUtilities.invokeLater(() -> {
            try {
                MenuUI menuUI = new MenuUI(currentUser, transactionService, summaryStatisticService, aiTransactionService, collegeStudentNeeds);
                JPanel mainPanel = menuUI.createMainPanel();

                JFrame frame = new JFrame("MenuUI Test Display");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(1200, 700);
                frame.add(mainPanel);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println("MenuUITest: MenuUI panel for user '" + currentUser.getUsername() + "' displayed.");
            } catch (Exception e) {
                System.err.println("MenuUITest: Error creating or displaying MenuUI.");
                e.printStackTrace();
            }
        });

        try {
            Thread.sleep(5000); // Keep frame visible for 5 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("MenuUITest: testDisplayMenuUI finished.");
    }
}