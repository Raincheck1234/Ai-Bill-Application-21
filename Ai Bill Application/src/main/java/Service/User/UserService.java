package Service.User;

import Constants.ConfigConstants;
import DAO.UserDao;
import DAO.TransactionDao;
import DAO.SummaryStatisticDao;
import DAO.Impl.CsvTransactionDao; // Need implementation to create initial files headers
import DAO.Impl.CsvSummaryStatisticDao; // Need implementation
import lombok.Builder;
import model.User;
import model.Transaction; // Needed for headers
import model.SummaryStatistic; // Needed for headers

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserService {
    private final UserDao userDao;
    // Inject DAOs needed to create initial user files
    private final TransactionDao transactionDao; // Used via CacheManager in other services, but needed here for file creation
    private final SummaryStatisticDao summaryStatisticDao; // Needed for initial file creation

    private final Map<String, User> userCache = new HashMap<>(); // Cache users in memory
    private final String userDataBaseDir; // Base directory for user data files

    /**
     * Constructor now accepts User, Transaction, and Summary DAOs.
     * @param userDao The UserDao instance.
     * @param transactionDao The TransactionDao instance (needed for initial file creation headers).
     * @param summaryStatisticDao The SummaryStatisticDao instance (needed for initial file creation headers).
     */
    public UserService(UserDao userDao, TransactionDao transactionDao, SummaryStatisticDao summaryStatisticDao) {
        this.userDao = userDao;
        this.transactionDao = transactionDao; // Assign injected DAO
        this.summaryStatisticDao = summaryStatisticDao; // Assign injected DAO
        this.userDataBaseDir = ConfigConstants.USER_DATA_BASE_DIR; // Get base directory from config
        System.out.println("UserService initialized. User data base directory: " + userDataBaseDir);

        // Basic validation for the base directory config
        if (this.userDataBaseDir == null || this.userDataBaseDir.trim().isEmpty()) {
            System.err.println("ERROR: USER_DATA_BASE_DIR is not configured correctly! File creation may fail.");
            // Depending on requirements, you might want to throw an error or exit here.
        }

        loadUsers(); // Load users when the service is initialized
    }

    /**
     * Loads all users into an in-memory cache.
     */
    private void loadUsers() {
        try {
            List<User> users = userDao.getAllUsers();
            userCache.clear(); // Clear previous cache
            for (User user : users) {
                userCache.put(user.getUsername(), user);
            }
            System.out.println("Loaded " + userCache.size() + " users into cache.");
        } catch (IOException e) {
            System.err.println("Failed to load users from data source.");
            e.printStackTrace();
            // Depending on requirements, you might want to exit or handle this more gracefully
            // For now, we'll allow the app to run with an empty user list, though login will fail for existing users.
        }
    }

    /**
     * Authenticates a user.
     *
     * @param username The username.
     * @param password The password.
     * @return The authenticated User object if successful, null otherwise.
     */
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }
        User user = userCache.get(username.trim
       ());
        if (user != null && user.getPassword().equals(password.trim())) { // Simple password check
            System.out.println("Authentication successful for user: " + username);
            return user; // Authentication successful, return the User object
        }
        System.out.println("Authentication failed for username: " + username);
        return null; // Authentication failed
    }

    /**
     * Retrieves a user by username from the cache.
     * @param username The username.
     * @return The User object or null if not found.
     */
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userCache.get(username.trim());
    }


    /**
     * Registers a new user.
     * @param username The desired username.
     * @param password The password.
     * @param role The role (e.g., "user").
     * @return true if registration was successful, false otherwise (e.g., username taken).
     * @throws Exception If file creation or saving fails.
     * @throws IllegalArgumentException If input data is invalid or username exists.
     */
    public boolean registerUser(String username, String password, String role) throws Exception {
        System.out.println("Attempting to register new user: " + username);

        // --- Validation ---
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Username, password, and role cannot be empty.");
        }
        // Optional: More password strength validation

        // --- Check if username already exists ---
        // loadUsers() caches existing users, check against the cache
        if (getUserByUsername(username) != null) {
            System.out.println("Registration failed: Username '" + username + "' already exists.");
            // Throw an IllegalArgumentException to indicate business logic failure
            throw new IllegalArgumentException("Username '" + username + "' already exists.");
            // Returning false is also an option depending on how the UI handles it,
            // but throwing an exception provides more detail to the caller.
        }

        // --- Generate file paths for the new user ---
        // Use a naming convention based on sanitized username
        String cleanUsername = username.trim().replaceAll("[^a-zA-Z0-9_.-]", "_"); // Sanitize username for file names
        // Ensure base directory is valid before using it
        if (userDataBaseDir == null || userDataBaseDir.trim().isEmpty()) {
            System.err.println("ERROR: USER_DATA_BASE_DIR is null or empty! Cannot generate file paths.");
            throw new IllegalStateException("User data base directory is not configured."); // Configuration error
        }

        // Construct the full paths using Paths.get for correct path handling across OS
        Path txDirPath = Paths.get(userDataBaseDir, "transactions");
        Path statsDirPath = Paths.get(userDataBaseDir, "stats");

        Path userTransactionFilePath = txDirPath.resolve("user_" + cleanUsername + ".csv");
        Path userSummaryFilePath = statsDirPath.resolve("user_" + cleanUsername + "_summary.csv");

        String userTransactionFilePathStr = userTransactionFilePath.toString();
        String userSummaryFilePathStr = userSummaryFilePath.toString();

        System.out.println("Generated file paths: Tx='" + userTransactionFilePathStr + "', Summary='" + userSummaryFilePathStr + "'");


        // --- Create parent directories if they don't exist ---
        try {
            Files.createDirectories(txDirPath);
            Files.createDirectories(statsDirPath);
            System.out.println("Ensured user data directories exist.");
        } catch (IOException e) {
            System.err.println("Failed to create user data directories: " + e.getMessage());
            throw new IOException("Failed to create data directories. " + e.getMessage(), e);
        }


        // --- Create empty transaction and summary files with headers ---
        try {
            // Transaction file header: Need the correct header string.
            // CsvTransactionDao has a writeTransactionsToCSV that writes header for empty list.
            // We need a CsvTransactionDao instance here.
            CsvTransactionDao tempTxDao = new CsvTransactionDao(); // Create local instance
            // Write an empty list to ensure header is written if file is new
            tempTxDao.writeTransactionsToCSV(userTransactionFilePathStr, List.of());
            System.out.println("Created new transaction file with header: " + userTransactionFilePathStr);


            // Summary file header: Need the correct header string.
            // CsvSummaryStatisticDao has a writeAllStatistics that writes header for empty list.
            CsvSummaryStatisticDao tempSummaryDao = new CsvSummaryStatisticDao(); // Create local instance
            // Write an empty list to ensure header is written if file is new
            tempSummaryDao.writeAllStatistics(userSummaryFilePathStr, List.of());
            System.out.println("Created new summary file with header: " + userSummaryFilePathStr);


        } catch (IOException e) {
            System.err.println("Failed to create user data files for user '" + username + "'. Rolling back...");
            // Optional: Clean up the transaction/summary files created if the second one fails
            try {
                Files.deleteIfExists(userTransactionFilePath);
                Files.deleteIfExists(userSummaryFilePath); // Clean up both if file creation fails
            } catch (IOException cleanupEx) {
                System.err.println("Failed during cleanup of partial user files: " + cleanupEx.getMessage());
            }
            throw new IOException("Failed to create data files for user: " + username + ". " + e.getMessage(), e);
        }

        User newUser = User.builder()
                .username(username)
                .password(password) // Store password directly (for simplicity in this project)
                .role(role)
                .transactionFilePath(userTransactionFilePathStr) // Use the generated string path
                .summaryFilePath(userSummaryFilePathStr) // Use the generated string path
                .build();
        System.out.println("Created new User object: " + newUser);


        // --- Add new user to users.csv ---
        try {
            userDao.addUser(newUser); // Call the DAO method to append to users.csv
            System.out.println("Added new user to users.csv.");

            // --- Update in-memory cache ---
            userCache.put(newUser.getUsername(), newUser); // Add the new user to the cache
            System.out.println("Added new user to in-memory cache.");


            System.out.println("User registration successful for '" + username + "'.");
            return true; // Registration successful

        } catch (IllegalArgumentException e) {
            // This catch block handles IllegalArgumentException specifically from userDao.addUser
            System.err.println("Invalid user data passed to UserDao: " + e.getMessage());
            // Re-throw with a more specific message about what caused the DAO validation to fail
            throw new IllegalArgumentException("Failed to save user information. " + e.getMessage(), e);
        }
        catch (IOException e) {
            System.err.println("Failed to add new user to users.csv for user '" + username + "'.");
            // Consider cleaning up the transaction/summary files created earlier
            // if this step fails, to avoid orphaned files.
            try {
                Files.deleteIfExists(userTransactionFilePath); // Clean up if saving to users.csv fails
                Files.deleteIfExists(userSummaryFilePath);
            } catch (IOException cleanupEx) {
                System.err.println("Failed during cleanup of partial user files: " + cleanupEx.getMessage());
            }
            throw new IOException("Failed to save user information to user list file. " + e.getMessage(), e);
        }
        catch (Exception e) { // Catch any other unexpected errors
            System.err.println("An unexpected error occurred during registration for user '" + username + "'.");
            e.printStackTrace();
            throw new Exception("An unexpected error occurred during registration. " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a user by username from the data source (not just cache).
     * Useful for checking existence without relying solely on potentially stale cache.
     * NOTE: This method bypasses the cache. Use getUserByUsername for cached access.
     * @param username The username.
     * @return The User object if found in the data source, null otherwise.
     * @throws IOException If an I/O error occurs.
     */
    public User getUserByUsernameFromDataSource(String username) throws IOException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        // Reload all users from the file to check for existence
        List<User> allUsers = userDao.getAllUsers();
        return allUsers.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst()
                .orElse(null);
    }

}