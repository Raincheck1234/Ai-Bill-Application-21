import javax.swing.*;

import Controller.MenuUI;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.Impl.CsvSummaryStatisticDao;
import DAO.UserDao;
import DAO.TransactionDao;
import DAO.SummaryStatisticDao;
import Interceptor.Login.LoginDialog;
import Service.Impl.TransactionServiceImpl;
import Service.Impl.SummaryStatisticService;
import Service.AIservice.AITransactionService; // Import AI Service classes
import Service.AIservice.CollegeStudentNeeds;
import Service.TransactionService;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

public class Main {
    public static void main(String[] args) {
        // Ensure ConfigConstants is loaded first
        String usersCsvPath = ConfigConstants.USERS_CSV_PATH;
        String summaryCsvPath = ConfigConstants.SUMMARY_CSV_PATH;
        System.out.println("Attempting to load users from: " + usersCsvPath);
        System.out.println("Summary statistics will be saved to: " + summaryCsvPath);


        // Initialize DAOs
        UserDao userDao = new CsvUserDao(usersCsvPath);
        TransactionDao transactionDao = new CsvTransactionDao();
        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao();


        // Initialize Services
        UserService userService = new UserService(userDao);
        // TransactionServiceImpl is initialized per user in MenuUI -> NO, initialize it here and pass it!
        // The TransactionServiceImpl instance *is* user-specific, so it's better to create it *after* login.
        // But AI Services need it *before* MenuUI is fully constructed and shows the panel.
        // Option 1: Pass TransactionService to MenuUI constructor and AI/CS services are initialized in MenuUI. (Current approach)
        // Option 2: Create AI/CS services here in Main and pass them to MenuUI. They would need the user-specific TS instance.
        // Let's stick with Option 1 for now, initializing AI/CS in MenuUI after getting TS.

        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao);


        // In the event dispatch thread (EDT) start GUI
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(userService);
            User authenticatedUser = loginDialog.showDialogAndGetResult();

            if (authenticatedUser != null) {
                System.out.println("Logged in as: " + authenticatedUser.getUsername() + " (" + authenticatedUser.getRole() + ")");
                System.out.println("User's transaction file: " + authenticatedUser.getTransactionFilePath());

                // Initialize TransactionServiceImpl *for the logged-in user*
                TransactionService transactionServiceForCurrentUser = new TransactionServiceImpl(authenticatedUser.getTransactionFilePath());

                // Initialize AI Services *with* the user-specific TransactionService
                // This is where AI/CS services are created with their dependency injected
                AITransactionService aiTransactionService = new AITransactionService(transactionServiceForCurrentUser);
                CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionServiceForCurrentUser);


                // Pass the authenticated user, their transaction service, summary statistic service, AND AI services to MenuUI
                MenuUI menuUI = new MenuUI(authenticatedUser, transactionServiceForCurrentUser, summaryStatisticService, aiTransactionService, collegeStudentNeeds); // Modify MenuUI constructor


                JFrame frame = new JFrame("交易管理系统 - " + authenticatedUser.getUsername());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 600);
                frame.setLocationRelativeTo(null);
                frame.add(menuUI.createMainPanel());
                frame.setVisible(true);
            } else {
                System.out.println("Login failed or cancelled. Exiting.");
                System.exit(0);
            }
        });
    }
}