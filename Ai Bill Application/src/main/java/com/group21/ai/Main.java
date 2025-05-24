package com.group21.ai;

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
import Service.AIservice.AITransactionService;
import Service.AIservice.CollegeStudentNeeds;
import Service.TransactionService;
import Service.User.UserService;
import com.formdev.flatlaf.FlatIntelliJLaf;
import model.User;
import Constants.ConfigConstants;

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors; // Import Executors
import java.util.concurrent.TimeUnit; // Import TimeUnit


public class Main {

    // Define a static ExecutorService for managing background tasks
    private static ExecutorService executorService; // Made static for easy access from Main

    public static void main(String[] args) {
        // Ensure ConfigConstants is loaded first
        String usersCsvPath = ConfigConstants.USERS_CSV_PATH;
        String summaryCsvPath = ConfigConstants.SUMMARY_CSV_PATH;
        String userDataBaseDir = ConfigConstants.USER_DATA_BASE_DIR;

        System.out.println("Attempting to load users from: " + usersCsvPath);
        System.out.println("User-specific data will be stored under: " + userDataBaseDir);

        // --- Initialize ExecutorService ---
        // Create a fixed-size thread pool, e.g., with 4 threads
        executorService = Executors.newFixedThreadPool(10);
        System.out.println("ExecutorService initialized with a fixed thread pool size of 4.");
        // --- End ExecutorService Initialization ---


        // --- Initialize FlatLaf Look and Feel ---
        try {
            UIManager.setLookAndFeel( new FlatIntelliJLaf() );
            System.out.println("FlatLaf Look and Feel initialized.");
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize FlatLaf Look and Feel" );
            ex.printStackTrace();
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
        UserService userService = new UserService(userDao, transactionDao, summaryStatisticDao);

        SummaryStatisticService summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryStatisticDao);


        // In the event dispatch thread (EDT) start GUI
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(userService);
            User authenticatedUser = loginDialog.showDialogAndGetResult();

            if (authenticatedUser != null) {
                System.out.println("Logged in as: " + authenticatedUser.getUsername() + " (" + authenticatedUser.getRole() + ")");
                System.out.println("User's transaction file: " + authenticatedUser.getTransactionFilePath());
                if (authenticatedUser.getSummaryFilePath() != null) {
                    System.out.println("User's summary file: " + authenticatedUser.getSummaryFilePath());
                }


                // Initialize TransactionServiceImpl *for the logged-in user*
                TransactionService transactionServiceForCurrentUser = new TransactionServiceImpl(authenticatedUser.getTransactionFilePath());

                // Initialize AI Services *with* the user-specific TransactionService
                // Also pass the ExecutorService to services/UI components that trigger tasks
                AITransactionService aiTransactionService = new AITransactionService(transactionServiceForCurrentUser);
                CollegeStudentNeeds collegeStudentNeeds = new CollegeStudentNeeds(transactionServiceForCurrentUser);


                // Pass all necessary service instances AND the executor service to MenuUI
                MenuUI menuUI = new MenuUI(authenticatedUser, transactionServiceForCurrentUser, summaryStatisticService, aiTransactionService, collegeStudentNeeds, executorService); // Modify MenuUI constructor


                JFrame frame = new JFrame("Transaction Management System - " + authenticatedUser.getUsername());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Use EXIT_ON_CLOSE to exit the application
                frame.setSize(1200, 600); // Reverted size for better general display
                frame.setLocationRelativeTo(null);
                frame.add(menuUI.createMainPanel()); // Get the main panel from MenuUI instance
                frame.setVisible(true);

                // --- Add a shutdown hook to gracefully close the ExecutorService on application exit ---
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutting down ExecutorService...");
                    executorService.shutdown(); // Initiate an orderly shutdown
                    try {
                        // Wait a reasonable time for tasks to complete
                        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                            System.err.println("ExecutorService did not terminate within the timeout. Forcing shutdown.");
                            executorService.shutdownNow(); // Forcefully shut down
                        } else {
                            System.out.println("ExecutorService shut down cleanly.");
                        }
                    } catch (InterruptedException e) {
                        System.err.println("ExecutorService shutdown interrupted.");
                        executorService.shutdownNow();
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                    }
                }));
                System.out.println("Shutdown hook added for ExecutorService.");
                // --- End Shutdown Hook ---

            } else {
                System.out.println("Login failed or cancelled. Exiting.");
                System.exit(0); // Exit on login failure or cancel
            }
        });
    }

    // Helper method to get the ExecutorService instance
    public static ExecutorService getExecutorService() {
        return executorService;
    }
}