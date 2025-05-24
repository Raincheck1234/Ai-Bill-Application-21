package Service.AIservice;

import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.Impl.TransactionServiceImpl;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.Test;

public class ColledgeStudentThreadTest { // Corrected class name

    @Test
    void testRunCollegeStudentThread() {
        System.out.println("CollegeStudentThreadTest: Running testRunCollegeStudentThread...");
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao, new CsvTransactionDao(), new CsvSummaryStatisticDao());
        User testUser = userService.authenticate("user1", "pass123"); // Student user

        if (testUser == null) {
            System.err.println("CollegeStudentThreadTest: Failed to authenticate test user 'user1'. Test cannot proceed.");
            return;
        }

        TransactionService transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionService);

        // Corrected instantiation of ColledgeStudentThread (as per original file name)
        // If the class name was intended to be CollegeStudentThread, adjust accordingly.
        ColledgeStudentThread studentThread = new ColledgeStudentThread(
                collegeStudentNeeds,
                testUser.getTransactionFilePath()
        );
        Thread thread = new Thread(studentThread);
        thread.start();
        try {
            thread.join(10000); // Wait for up to 10 seconds
            System.out.println("CollegeStudentThreadTest: Thread finished or timed out.");
        } catch (InterruptedException e) {
            System.err.println("CollegeStudentThreadTest: Thread interrupted.");
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        System.out.println("CollegeStudentThreadTest: testRunCollegeStudentThread finished.");
    }
}