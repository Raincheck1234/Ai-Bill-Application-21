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

public class ButtonEditorTest {

    @Test
    void testButtonEditorInstantiationAndUsage() {
        // Full setup for MenuUI is required because ButtonEditor depends on it.
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        User testUser = userService.authenticate("user1", "pass123");

        if (testUser == null) {
            System.err.println("ButtonEditorTest: Failed to authenticate test user 'user1'. Test cannot proceed.");
            return;
        }

        TransactionDao transactionDao = new CsvTransactionDao();
        TransactionService transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao();
        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao);
        AITransactionService aiTransactionService = new AITransactionService(transactionService);
        CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionService);

        // MenuUI needs to be created on EDT if it does heavy UI work in constructor,
        // but its constructor seems okay for direct call here.
        // For safety, let's ensure any UI part of MenuUI setup is on EDT if it were an issue.
        // However, the constructor itself doesn't build the final panel yet.
        MenuUI menuUI = new MenuUI(testUser, transactionService, summaryStatisticService, aiTransactionService, collegeStudentNeeds);
        // We don't need to display MenuUI itself for this ButtonEditor test.

        try {
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
        }
        System.out.println("ButtonEditorTest: testButtonEditorInstantiationAndUsage finished.");
    }
}