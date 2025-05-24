package Service.AIservice;

import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.Test;

public class AIAnalyzerThreadTest {

    @Test
    void testRunAIAnalyzerThread() {
        System.out.println("AIAnalyzerThreadTest: Running testRunAIAnalyzerThread...");
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        User testUser = userService.authenticate("admin", "admin123");

        if (testUser == null) {
            System.err.println("AIAnalyzerThreadTest: Failed to authenticate test user 'admin'. Test cannot proceed.");
            return;
        }

        TransactionService transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        AITransactionService aiService = new AITransactionService(transactionService);

        AIAnalyzerThread analyzerThread = new AIAnalyzerThread(
                aiService,
                "Analyze my spending",
                testUser.getTransactionFilePath(),
                "2025/03/01",
                ""
        );
        Thread thread = new Thread(analyzerThread);
        thread.start();
        try {
            thread.join(10000); // Wait for up to 10 seconds
            System.out.println("AIAnalyzerThreadTest: Thread finished or timed out.");
        } catch (InterruptedException e) {
            System.err.println("AIAnalyzerThreadTest: Thread interrupted.");
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        System.out.println("AIAnalyzerThreadTest: testRunAIAnalyzerThread finished.");
    }
}