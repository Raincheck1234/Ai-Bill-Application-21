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

// Import FlatLaf class
import com.formdev.flatlaf.FlatIntelliJLaf; // Or FlatLaf, FlatDarculaLaf, etc.

public class Main {
    public static void main(String[] args) {
        // Ensure ConfigConstants is loaded first
        String usersCsvPath = ConfigConstants.USERS_CSV_PATH;
        String summaryCsvPath = ConfigConstants.SUMMARY_CSV_PATH; // This might be admin global path
        String userDataBaseDir = ConfigConstants.USER_DATA_BASE_DIR; // Base directory for user data files


        System.out.println("Attempting to load users from: " + usersCsvPath);
        System.out.println("User-specific data will be stored under: " + userDataBaseDir);


        // --- Initialize FlatLaf Look and Feel ---
        try {
            // You can use different FlatLaf themes, like FlatLaf(), FlatDarculaLaf(), FlatLightLaf(), FlatDarkLaf()
            // FlatIntelliJLaf is often a good starting point for a modern look
            UIManager.setLookAndFeel( new FlatIntelliJLaf() );
            System.out.println("FlatLaf Look and Feel initialized.");
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize FlatLaf Look and Feel" );
            ex.printStackTrace();
            // Handle the error - maybe set default Look and Feel or exit
            // For robustness, you can set the system default L&F as a fallback:
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("Set System Look and Feel as fallback.");
            } catch (Exception e) {
                System.err.println("Failed to set System Look and Feel.");
            }
        }
        // --- End FlatLaf Initialization ---


        // Initialize DAOs
        UserDao userDao = new CsvUserDao(usersCsvPath);
        TransactionDao transactionDao = new CsvTransactionDao();
        SummaryStatisticDao summaryStatisticDao = new CsvSummaryStatisticDao();


        // Initialize Services
        // Inject DAOs into UserService
        // Note: You mentioned inject TransactionDao and SummaryStatisticDao into UserService in Step 9.2.
        // Let's confirm if UserService constructor was updated. If not, this line might cause error.
        // Assuming UserService constructor now accepts TransactionDao and SummaryStatisticDao:
        UserService userService = new UserService(userDao, transactionDao, summaryStatisticDao);


        // SummaryStatisticService also needs these DAOs
        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao);


        // In the event dispatch thread (EDT) start GUI
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(userService);
            User authenticatedUser = loginDialog.showDialogAndGetResult();

            if (authenticatedUser != null) {
                System.out.println("Logged in as: " + authenticatedUser.getUsername() + " (" + authenticatedUser.getRole() + ")");
                System.out.println("User's transaction file: " + authenticatedUser.getTransactionFilePath());
                if (authenticatedUser.getSummaryFilePath() != null) { // Check if summary path exists
                    System.out.println("User's summary file: " + authenticatedUser.getSummaryFilePath());
                }


                // Initialize TransactionServiceImpl *for the logged-in user*
                TransactionService transactionServiceForCurrentUser = new TransactionServiceImpl(authenticatedUser.getTransactionFilePath());

                // Initialize AI Services *with* the user-specific TransactionService
                // Assuming AITransactionService and CollegeStudentNeeds constructors accept TransactionService
                AITransactionService aiTransactionService = new AITransactionService(transactionServiceForCurrentUser);
                CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionServiceForCurrentUser);


                // Pass all necessary service instances to MenuUI
                // Assuming MenuUI constructor accepts all these services
                MenuUI menuUI = new MenuUI(authenticatedUser, transactionServiceForCurrentUser, summaryStatisticService, aiTransactionService, collegeStudentNeeds);


                JFrame frame = new JFrame("Transaction Management System - " + authenticatedUser.getUsername()); // Include username in title
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 600); // Reverted size, 2000x1500 is very large
                frame.setLocationRelativeTo(null);
                frame.add(menuUI.createMainPanel()); // Get the main panel from MenuUI instance
                frame.setVisible(true);
            } else {
                System.out.println("Login failed or cancelled. Exiting.");
                System.exit(0); // Exit on login failure or cancel
            }
        });
    }

    // showMainUI method is not used anymore
    // private static void showMainUI(JPanel panel) { ... }
}