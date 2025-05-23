package Service.AIservice;

import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.Transaction;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CollegeStudentNeedsTest {

    private CollegeStudentNeeds collegeStudentNeeds;
    private User testUser;
    private TransactionService transactionService; // For CollegeStudentNeeds dependency

    @BeforeEach
    void setUp() {
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        testUser = userService.authenticate("user1", "pass123"); // Assuming user1 might be a student

        if (testUser == null) {
            System.err.println("CollegeStudentNeedsTest: Failed to authenticate test user 'user1'. Using admin.");
            testUser = userService.authenticate("admin", "admin123");
        }
        if (testUser == null) {
            throw new IllegalStateException("CollegeStudentNeedsTest: Cannot authenticate any test user. Aborting setup.");
        }

        transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        collegeStudentNeeds = new CollegeStudentNeeds(transactionService); // Inject dependency
    }

    @Test
    void testRecognizeTransaction() {
        System.out.println("CollegeStudentNeedsTest: Running testRecognizeTransaction...");
        try {
            Transaction sampleTransaction = new Transaction(
                    "2025/03/15 12:00", "午餐", "学校食堂", "红烧肉套餐", "支出",
                    15.0, "校园卡", "已完成", "STU001", "SCH001", "普通午饭"
            );
            String category = collegeStudentNeeds.RecognizeTransaction(sampleTransaction);
            System.out.println("CollegeStudentNeedsTest (RecognizeTransaction) Suggested Category: " + category);
        } catch (Exception e) {
            System.err.println("CollegeStudentNeedsTest (RecognizeTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CollegeStudentNeedsTest: testRecognizeTransaction finished.");
    }

    @Test
    void testGenerateTipsForSaving() {
        System.out.println("CollegeStudentNeedsTest: Running testGenerateTipsForSaving...");
        try {
            String tips = collegeStudentNeeds.generateTipsForSaving(testUser.getTransactionFilePath());
            System.out.println("CollegeStudentNeedsTest (generateTipsForSaving) Tips: " + tips);
        } catch (Exception e) {
            System.err.println("CollegeStudentNeedsTest (generateTipsForSaving): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CollegeStudentNeedsTest: testGenerateTipsForSaving finished.");
    }

    @Test
    void testGenerateBudget() {
        System.out.println("CollegeStudentNeedsTest: Running testGenerateBudget...");
        try {
            double[] budget = collegeStudentNeeds.generateBudget(testUser.getTransactionFilePath());
            if (budget != null && budget.length == 2) {
                System.out.println("CollegeStudentNeedsTest (generateBudget) Budget: [" + budget[0] + ", " + budget[1] + "]");
            } else {
                System.out.println("CollegeStudentNeedsTest (generateBudget) Budget: Could not be generated or invalid format.");
            }
        } catch (Exception e) {
            System.err.println("CollegeStudentNeedsTest (generateBudget): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CollegeStudentNeedsTest: testGenerateBudget finished.");
    }
}