package Service.User;

import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        userService = new UserService(userDao, new CsvTransactionDao(), new CsvSummaryStatisticDao());
    }

    @Test
    void testAuthenticate_Success() {
        System.out.println("UserServiceTest: Running testAuthenticate_Success...");
        User user = userService.authenticate("admin", "admin123");
        if (user != null) {
            System.out.println("UserServiceTest (auth_success): Authenticated user: " + user.getUsername() + " with role: " + user.getRole());
        } else {
            System.err.println("UserServiceTest (auth_success): Authentication FAILED for admin.");
        }
        System.out.println("UserServiceTest: testAuthenticate_Success finished.");
    }

    @Test
    void testAuthenticate_Failure_WrongPassword() {
        System.out.println("UserServiceTest: Running testAuthenticate_Failure_WrongPassword...");
        User user = userService.authenticate("user1", "wrongpassword");
        if (user == null) {
            System.out.println("UserServiceTest (auth_fail_pwd): Authentication correctly failed for user1 with wrong password.");
        } else {
            System.err.println("UserServiceTest (auth_fail_pwd): Authentication INCORRECTLY succeeded for user1 with wrong password.");
        }
        System.out.println("UserServiceTest: testAuthenticate_Failure_WrongPassword finished.");
    }

    @Test
    void testAuthenticate_Failure_UnknownUser() {
        System.out.println("UserServiceTest: Running testAuthenticate_Failure_UnknownUser...");
        User user = userService.authenticate("unknownuser", "somepassword");
        if (user == null) {
            System.out.println("UserServiceTest (auth_fail_user): Authentication correctly failed for unknownuser.");
        } else {
            System.err.println("UserServiceTest (auth_fail_user): Authentication INCORRECTLY succeeded for unknownuser.");
        }
        System.out.println("UserServiceTest: testAuthenticate_Failure_UnknownUser finished.");
    }

    @Test
    void testGetUserByUsername() {
        System.out.println("UserServiceTest: Running testGetUserByUsername...");
        User user = userService.getUserByUsername("user2");
        if (user != null) {
            System.out.println("UserServiceTest (getByUsername): Found user: " + user.getUsername() + ", File: " + user.getTransactionFilePath());
        } else {
            System.err.println("UserServiceTest (getByUsername): User 'user2' NOT FOUND.");
        }

        User nonExistentUser = userService.getUserByUsername("no_such_user");
        if (nonExistentUser == null) {
            System.out.println("UserServiceTest (getByUsername): Correctly did not find 'no_such_user'.");
        } else {
            System.err.println("UserServiceTest (getByUsername): INCORRECTLY found 'no_such_user'.");
        }
        System.out.println("UserServiceTest: testGetUserByUsername finished.");
    }
}