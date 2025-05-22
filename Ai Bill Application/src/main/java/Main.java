import javax.swing.*;

import Controller.MenuUI;
import DAO.Impl.CsvTransactionDao; // Use Impl package
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao; // Use Impl package
import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Interceptor.Login.LoginDialog;
import Service.Impl.TransactionServiceImpl;
import Service.User.UserService;
import model.User;
import Constants.ConfigConstants;

public class Main {
    public static void main(String[] args) {
        // Ensure ConfigConstants is loaded first
        String usersCsvPath = ConfigConstants.USERS_CSV_PATH;
        System.out.println("Attempting to load users from: " + usersCsvPath);

        // Initialize User Service (DAO for users is needed)
        UserDao userDao = new CsvUserDao(usersCsvPath); // Pass the user CSV path
        UserService userService = new UserService(userDao);

        // Transaction Service DAO (instance created within TransactionServiceImpl)
        // Remove the static initialization line: TransactionServiceImpl.csvTransactionDao = new CsvTransactionDao();


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
                // Pass the user's transaction file path to the service constructor
                TransactionServiceImpl transactionServiceForCurrentUser = new TransactionServiceImpl(authenticatedUser.getTransactionFilePath());

                // Pass the authenticated user AND their specific transaction service to MenuUI
                // Modify MenuUI constructor again to accept TransactionServiceImpl
                MenuUI menuUI = new MenuUI(authenticatedUser, transactionServiceForCurrentUser); // Modify MenuUI constructor

                JPanel mainPanel = menuUI.createMainPanel();
                showMainUI(mainPanel);
            } else {
                System.out.println("Login failed or cancelled. Exiting.");
                System.exit(0);
            }
        });
    }

    /**
     * Displays the main application window.
     */
    private static void showMainUI(JPanel panel) {
        JFrame frame = new JFrame("交易管理系统");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);
    }
}