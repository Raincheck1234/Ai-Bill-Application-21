package Service.AIservice;


import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AITransactionServiceTest {

    private AITransactionService aiTransactionService;
    private TransactionService transactionService; // For AITransactionService dependency
    private User testUser;

    @BeforeEach
    void setUp() {
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        testUser = userService.authenticate("user1", "pass123"); // Use a common test user

        if (testUser == null) {
            System.err.println("AITransactionServiceTest: Failed to authenticate test user 'user1'. Using admin.");
            testUser = userService.authenticate("admin", "admin123");
        }
        if (testUser == null) {
            throw new IllegalStateException("AITransactionServiceTest: Cannot authenticate any test user. Aborting setup.");
        }

        transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        aiTransactionService = new AITransactionService(transactionService); // Inject dependency
    }

    @Test
    void testAnalyzeTransactions() {
        System.out.println("AITransactionServiceTest: Running testAnalyzeTransactions...");
        try {
            String result = aiTransactionService.analyzeTransactions(
                    "帮我分析一下最近的支出",
                    testUser.getTransactionFilePath(),
                    "2025/03/01",
                    "2025/03/31"
            );
            System.out.println("AITransactionServiceTest (analyzeTransactions) Result: " + result);
        } catch (Exception e) {
            System.err.println("AITransactionServiceTest (analyzeTransactions): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("AITransactionServiceTest: testAnalyzeTransactions finished.");
    }

    @Test
    void testGeneratePersonalSummary() {
        System.out.println("AITransactionServiceTest: Running testGeneratePersonalSummary...");
        try {
            String result = aiTransactionService.generatePersonalSummary(testUser.getTransactionFilePath());
            System.out.println("AITransactionServiceTest (generatePersonalSummary) Result: " + result);
        } catch (Exception e) {
            System.err.println("AITransactionServiceTest (generatePersonalSummary): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("AITransactionServiceTest: testGeneratePersonalSummary finished.");
    }

    @Test
    void testSuggestSavingsGoals() {
        System.out.println("AITransactionServiceTest: Running testSuggestSavingsGoals...");
        try {
            String result = aiTransactionService.suggestSavingsGoals(testUser.getTransactionFilePath());
            System.out.println("AITransactionServiceTest (suggestSavingsGoals) Result: " + result);
        } catch (Exception e) {
            System.err.println("AITransactionServiceTest (suggestSavingsGoals): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("AITransactionServiceTest: testSuggestSavingsGoals finished.");
    }

    @Test
    void testGivePersonalSavingTips() {
        System.out.println("AITransactionServiceTest: Running testGivePersonalSavingTips...");
        try {
            String result = aiTransactionService.givePersonalSavingTips(testUser.getTransactionFilePath());
            System.out.println("AITransactionServiceTest (givePersonalSavingTips) Result: " + result);
        } catch (Exception e) {
            System.err.println("AITransactionServiceTest (givePersonalSavingTips): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("AITransactionServiceTest: testGivePersonalSavingTips finished.");
    }
}