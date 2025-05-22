import javax.swing.*;

import Controller.MenuUI;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.Impl.CsvSummaryStatisticDao; // Import SummaryStatisticDao implementation
import DAO.UserDao;
import DAO.TransactionDao; // Import interfaces
import DAO.SummaryStatisticDao;
import Interceptor.Login.LoginDialog;
import Service.Impl.TransactionServiceImpl;
import Service.Impl.SummaryStatisticService; // Import SummaryStatisticService
import Service.TransactionService; // Import interface
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

public class Main {
    public static void main(String[] args) {
        // Ensure ConfigConstants is loaded first
        String usersCsvPath = ConfigConstants.USERS_CSV_PATH;
        String summaryCsvPath = ConfigConstants.SUMMARY_CSV_PATH; // Get summary path
        System.out.println("Attempting to load users from: " + usersCsvPath);
        System.out.println("Summary statistics will be saved to: " + summaryCsvPath);


        // Initialize DAOs
        UserDao userDao = new CsvUserDao(usersCsvPath); // Pass the user CSV path
        TransactionDao transactionDao = new CsvTransactionDao(); // Needs to be available for CacheManager loader
        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao(); // Pass the summary CSV path if needed by its constructor (CsvSummaryStatisticDao doesn't need path in constructor)


        // Initialize Services
        UserService userService = new UserService(userDao);
        // TransactionServiceImpl is initialized per user in MenuUI
        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao); // Initialize SummaryStatisticService


        // In the event dispatch thread (EDT) start GUI
        SwingUtilities.invokeLater(() -> {
            // 1. Show Login Dialog
            LoginDialog loginDialog = new LoginDialog(userService);
            User authenticatedUser = loginDialog.showDialogAndGetResult();

            // 2. Show main interface only if login was successful
            if (authenticatedUser != null) {
                System.out.println("Logged in as: " + authenticatedUser.getUsername() + " (" + authenticatedUser.getRole() + ")");
                System.out.println("User's transaction file: " + authenticatedUser.getTransactionFilePath());

                // Initialize TransactionServiceImpl *for the logged-in user*
                TransactionService transactionServiceForCurrentUser = new TransactionServiceImpl(authenticatedUser.getTransactionFilePath());

                // Pass the authenticated user, their transaction service, AND the summary statistic service to MenuUI
                MenuUI menuUI = new MenuUI(authenticatedUser, transactionServiceForCurrentUser, summaryStatisticService); // Modify MenuUI constructor

                JFrame frame = new JFrame("交易管理系统 - " + authenticatedUser.getUsername()); // Include username in title
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 600);
                frame.setLocationRelativeTo(null);
                frame.add(menuUI.createMainPanel()); // Add the main panel from MenuUI
                frame.setVisible(true);
            } else {
                System.out.println("Login failed or cancelled. Exiting.");
                System.exit(0);
            }
        });
    }

    // showMainUI is no longer needed as we build the frame directly in main
    // private static void showMainUI(JPanel panel) { ... }
}