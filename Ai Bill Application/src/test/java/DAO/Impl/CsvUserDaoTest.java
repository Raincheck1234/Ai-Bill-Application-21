package DAO.Impl;

import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import model.User;
import Constants.ConfigConstants; // For USERS_CSV_PATH

import org.junit.jupiter.api.Test;
import java.util.List;

public class CsvUserDaoTest {

    @Test
    void testGetAllUsers() {
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        try {
            List<User> users = userDao.getAllUsers();
            System.out.println("CsvUserDaoTest: Loaded " + users.size() + " users.");
            if (!users.isEmpty()) {
                System.out.println("CsvUserDaoTest: First user: " + users.get(0));
            }
            // No assertions, just checking if it runs
        } catch (Exception e) {
            System.err.println("CsvUserDaoTest: Error in testGetAllUsers.");
            e.printStackTrace();
            // To make JUnit fail the test if an exception occurs
            throw new RuntimeException("Test failed due to exception in getAllUsers", e);
        }
        System.out.println("CsvUserDaoTest: testGetAllUsers finished successfully.");
    }
}