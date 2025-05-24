package Controller;

import Controller.ButtonEditor;
import Controller.MenuUI;
import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.SummaryStatisticDao;
import DAO.TransactionDao;
import DAO.UserDao;
import Service.AIservice.AITransactionService;
import Service.AIservice.CollegeStudentNeeds;
import Service.Impl.SummaryStatisticService;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors; // Import Executors
import java.util.concurrent.TimeUnit; // Import TimeUnit


public class ButtonEditorTest {

    // Use a dedicated ExecutorService for tests or access Main's if reliable
    // Accessing Main's static ExecutorService is simpler for testing purposes
    // Make sure Main class is initialized in the test environment if accessing its static members.
    // In a real test framework setup, you might manage a test-specific ExecutorService.

    @Test
    void testButtonEditorInstantiationAndUsage() {
        System.out.println("ButtonEditorTest: Running testButtonEditorInstantiationAndUsage...");

        // Setup services and user
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao, new CsvTransactionDao(), new CsvSummaryStatisticDao()); // Assuming UserService constructor is updated

        User testUser = userService.authenticate("user1", "pass123");

        if (testUser == null) {
            System.err.println("ButtonEditorTest: Failed to authenticate test user 'user1'. Attempting admin.");
            testUser = userService.authenticate("admin", "admin123");
        }
        if (testUser == null) {
            System.err.println("ButtonEditorTest: Cannot authenticate any test user. Test cannot proceed.");
            // You might need to ensure test users and their files exist as part of the test setup.
            return; // Abort test if no user
        }

        TransactionDao transactionDao = new CsvTransactionDao();
        TransactionService transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());

        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao();
        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao); // Assuming constructor updated

        AITransactionService aiTransactionService = new AITransactionService(transactionService); // Assuming constructor updated
        CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionService); // Assuming constructor updated

        // --- Get or create ExecutorService for testing ---
        // Option 1: Use Main's static ExecutorService (Requires Main to be initialized)
        // ExecutorService testExecutorService = Main.getExecutorService(); // Requires Main to be initialized AND getExecutorService() to be public

        // Option 2: Create a test-specific ExecutorService for this test class or method
        // For simplicity, let's create one specific to this test method.
        ExecutorService testExecutorService = Executors.newSingleThreadExecutor(); // Use a small pool for test


        try {
            // --- Modify MenuUI initialization to pass ExecutorService ---
            MenuUI menuUI = new MenuUI(testUser, transactionService, summaryStatisticService, aiTransactionService, collegeStudentNeeds, testExecutorService, new UserService(new CsvUserDao(""), new CsvTransactionDao(), new CsvSummaryStatisticDao())); // Pass ExecutorService


            ButtonEditor editor = new ButtonEditor(menuUI); // Pass the MenuUI instance
            // Simulate a call from JTable
            Component component = editor.getTableCellEditorComponent(
                    null,           // JTable (can be null for basic test)
                    "Edit Button",  // value
                    false,          // isSelected
                    0,              // row
                    0               // column
            );
            System.out.println("ButtonEditorTest: ButtonEditor instantiated and getTableCellEditorComponent called.");
            Object editorValue = editor.getCellEditorValue();
            System.out.println("ButtonEditorTest: getCellEditorValue returned: " + editorValue);

            if (component instanceof JPanel) {
                System.out.println("ButtonEditorTest: Editor component is a JPanel as expected.");
            } else {
                System.err.println("ButtonEditorTest: Editor component is NOT a JPanel.");
            }

        } catch (Exception e) {
            System.err.println("ButtonEditorTest: Error during test.");
            e.printStackTrace();
            throw e; // Re-throw to make JUnit mark it as failed
        } finally {
            // Shut down the test-specific ExecutorService
            if (testExecutorService != null) {
                testExecutorService.shutdown();
                try {
                    if (!testExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        System.err.println("ButtonEditorTest: Test ExecutorService did not terminate.");
                    }
                } catch (InterruptedException e) {
                    System.err.println("ButtonEditorTest: Test ExecutorService shutdown interrupted.");
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("ButtonEditorTest: testButtonEditorInstantiationAndUsage finished.");
    }
}