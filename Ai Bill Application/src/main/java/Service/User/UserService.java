package Service.User;

import Constants.ConfigConstants;
import DAO.UserDao;
import DAO.TransactionDao;
import DAO.SummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvSummaryStatisticDao;
import model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList; // Added for List.of() which returns immutable list, sometimes mutable needed
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections; // Added for Collections.unmodifiableList


public class UserService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final SummaryStatisticDao summaryStatisticDao;

    private final Map<String, User> userCache = new HashMap<>(); // Cache users in memory
    private final String userDataBaseDir;

    /**
     * Constructor now accepts User, Transaction, and Summary DAOs.
     */
    public UserService(UserDao userDao, TransactionDao transactionDao, SummaryStatisticDao summaryStatisticDao) {
        this.userDao = userDao;
        this.transactionDao = transactionDao;
        this.summaryStatisticDao = summaryStatisticDao;
        this.userDataBaseDir = ConfigConstants.USER_DATA_BASE_DIR;
        System.out.println("UserService initialized. User data base directory: " + userDataBaseDir);

        if (this.userDataBaseDir == null || this.userDataBaseDir.trim().isEmpty()) {
            System.err.println("ERROR: USER_DATA_BASE_DIR is not configured correctly! File creation may fail.");
        }

        loadUsers(); // Load users when the service is initialized
    }

    /**
     * Loads all users into an in-memory cache.
     */
    private void loadUsers() {
        try {
            List<User> users = userDao.getAllUsers();
            userCache.clear();
            for (User user : users) {
                userCache.put(user.getUsername(), user);
            }
            System.out.println("Loaded " + userCache.size() + " users into cache.");
        } catch (IOException e) {
            System.err.println("Failed to load users from data source.");
            e.printStackTrace();
            // Decide how to handle critical errors like failing to load users.
            // For now, just log and continue with empty cache.
        }
    }

    /**
     * Gets all users. Returns a defensive copy from cache.
     *
     * @return A list of all users.
     * @throws IOException If loading from source fails (should ideally be handled by loadUsers).
     */
    // ADD THIS PUBLIC METHOD
    public List<User> getAllUsers() throws IOException {
        // Return a copy of the cached users to prevent external modification of the cache content.
        // loadUsers() handles IOException when reading from file initially.
        // If cache is empty, maybe attempt to load again? Or rely on initial load?
        // Let's rely on initial load and return from cache.
        if (userCache.isEmpty()) {
            // If cache is empty, try loading users again in case initial load failed
            loadUsers(); // This might re-throw or log.
        }
        // Return an unmodifiable list to prevent modification of the returned list.
        return Collections.unmodifiableList(new ArrayList<>(userCache.values()));
    }


    /**
     * Authenticates a user.
     */
    public User authenticate(String username, String password) {
        // ... existing implementation ...
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }

        User user = userCache.get(username.trim());
        if (user != null && user.getPassword().equals(password.trim())) {
            System.out.println("Authentication successful for user: " + username);
            return user;
        }
        System.out.println("Authentication failed for username: " + username);
        return null;
    }

    /**
     * Retrieves a user by username from the cache.
     */
    public User getUserByUsername(String username) {
        // ... existing implementation ...
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userCache.get(username.trim());
    }


    /**
     * Registers a new user.
     */
    public boolean registerUser(String username, String password, String role) throws Exception {
        System.out.println("Attempting to register new user: " + username);

        // --- Validation ---
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Username, password, and role cannot be empty.");
        }

        // --- Check if username already exists ---
        // Check against the most up-to-date source (file) to avoid race conditions if users.csv is modified externally or by multiple app instances
        try {
            if (getUserByUsernameFromDataSource(username) != null) {
                System.out.println("Registration failed: Username '" + username + "' already exists (checked file).");
                throw new IllegalArgumentException("Username '" + username + "' already exists.");
            }
        } catch (IOException e) {
            System.err.println("Error checking file for user existence during registration: " + e.getMessage());
            throw new IOException("Error checking user existence during registration. " + e.getMessage(), e);
        }


        // --- Generate file paths for the new user ---
        String cleanUsername = username.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (userDataBaseDir == null || userDataBaseDir.trim().isEmpty()) {
            System.err.println("ERROR: USER_DATA_BASE_DIR is null or empty! Cannot generate file paths.");
            throw new IllegalStateException("User data base directory is not configured.");
        }

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
            // Use CsvTransactionDao write method to create file with header
            CsvTransactionDao tempTxDao = new CsvTransactionDao();
            tempTxDao.writeTransactionsToCSV(userTransactionFilePathStr, List.of());
            System.out.println("Created new transaction file with header: " + userTransactionFilePathStr);

            // Use CsvSummaryStatisticDao write method to create file with header
            CsvSummaryStatisticDao tempSummaryDao = new CsvSummaryStatisticDao();
            tempSummaryDao.writeAllStatistics(userSummaryFilePathStr, List.of());
            System.out.println("Created new summary file with header: " + userSummaryFilePathStr);

        } catch (IOException e) {
            System.err.println("Failed to create user data files for user '" + username + "'. Rolling back...");
            try {
                Files.deleteIfExists(userTransactionFilePath);
                Files.deleteIfExists(userSummaryFilePath);
            } catch (IOException cleanupEx) {
                System.err.println("Failed during cleanup of partial user files: " + cleanupEx.getMessage());
            }
            throw new IOException("Failed to create data files for user: " + username + ". " + e.getMessage(), e);
        }


        // --- Create new User object ---
        User newUser = new User(
                username.trim(),
                password.trim(), // Store password directly
                role.trim(),
                userTransactionFilePathStr,
                userSummaryFilePathStr
        );
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
            // This catch block handles IllegalArgumentException specifically from userDao.addUser validation
            System.err.println("Invalid user data passed to UserDao: " + e.getMessage());
            throw new IllegalArgumentException("Failed to save user information. " + e.getMessage(), e);
        }
        catch (IOException e) {
            System.err.println("Failed to add new user to users.csv for user '" + username + "'.");
            try {
                Files.deleteIfExists(userTransactionFilePath);
                Files.deleteIfExists(userSummaryFilePath);
            } catch (IOException cleanupEx) {
                System.err.println("Failed during cleanup of partial user files: " + cleanupEx.getMessage());
            }
            throw new IOException("Failed to save user information to user list file. " + e.getMessage(), e);
        }
        catch (Exception e) {
            System.err.println("An unexpected error occurred during registration for user '" + username + "'.");
            e.printStackTrace();
            throw new Exception("An unexpected error occurred during registration. " + e.getMessage(), e);
        }
    }


    /**
     * Deletes a user.
     */
    public boolean deleteUser(String username) throws IOException, IllegalArgumentException {
        // ... existing implementation ...
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty for deletion.");
        }
        String usernameToDel = username.trim();
        System.out.println("Attempting to delete user: " + usernameToDel);

        // 1. Find the user in the cache to get file paths before deleting from file
        User userToDelete = userCache.get(usernameToDel);
        // If not in cache, check file directly (ensure cache is refreshed periodically or rely on file check)
        if (userToDelete == null) {
            try {
                userToDelete = getUserByUsernameFromDataSource(usernameToDel); // Check file directly
                if (userToDelete == null) {
                    System.out.println("User '" + usernameToDel + "' not found in file for deletion.");
                    return false; // User not found
                }
                // If found in file but not cache, add to cache? No, cache is just for performance.
            } catch (IOException e) {
                System.err.println("Error checking file for user existence during deletion: " + e.getMessage());
                throw new IOException("Error checking user existence during deletion. " + e.getMessage(), e);
            }
        }


        // 2. Delete user from users.csv via DAO
        boolean deletedFromList;
        try {
            deletedFromList = userDao.deleteUser(usernameToDel);
            System.out.println("User '" + usernameToDel + "' deleted from users.csv: " + deletedFromList);
        } catch (IOException e) {
            System.err.println("Error deleting user from users.csv: " + e.getMessage());
            throw new IOException("Error deleting user from user list file. " + e.getMessage(), e);
        }

        if (deletedFromList) {
            // 3. Attempt to delete associated data files
            System.out.println("Attempting to delete data files for user: " + userToDelete.getUsername());
            try {
                if (userToDelete.getTransactionFilePath() != null && !userToDelete.getTransactionFilePath().trim().isEmpty()) {
                    Path txPath = Paths.get(userToDelete.getTransactionFilePath());
                    boolean txDeleted = Files.deleteIfExists(txPath);
                    System.out.println("Transaction file " + txPath + " deleted: " + txDeleted);
                }
                if (userToDelete.getSummaryFilePath() != null && !userToDelete.getSummaryFilePath().trim().isEmpty()) {
                    Path summaryPath = Paths.get(userToDelete.getSummaryFilePath());
                    boolean summaryDeleted = Files.deleteIfExists(summaryPath);
                    System.out.println("Summary file " + summaryPath + " deleted: " + summaryDeleted);
                }
            } catch (IOException e) {
                System.err.println("Error deleting user data files for '" + usernameToDel + "': " + e.getMessage());
            }

            // 4. Remove user from in-memory cache
            userCache.remove(usernameToDel);
            System.out.println("User '" + usernameToDel + "' removed from cache.");

            return true; // Deletion from list was successful
        } else {
            System.out.println("User '" + usernameToDel + "' was not deleted from list by DAO (possibly not found).");
            return false;
        }
    }

    /**
     * Updates an existing user's information.
     */
    public boolean updateUser(User updatedUser) throws IOException, IllegalArgumentException {
        // ... existing implementation ...
        if (updatedUser == null || updatedUser.getUsername() == null || updatedUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Updated user object and username cannot be null or empty.");
        }
        String usernameToUpdate = updatedUser.getUsername().trim();
        System.out.println("Attempting to update user: " + usernameToUpdate);

        // 1. Check if user exists before attempting to update
        try {
            if (getUserByUsernameFromDataSource(usernameToUpdate) == null) {
                System.out.println("User '" + usernameToUpdate + "' not found for update (checked file).");
                throw new IllegalArgumentException("User '" + usernameToUpdate + "' not found."); // User not found business logic error
            }
        } catch (IOException e) {
            System.err.println("Error checking file for user existence during update: " + e.getMessage());
            throw new IOException("Error checking user existence during update. " + e.getMessage(), e);
        }


        // 2. Update user in users.csv via DAO
        boolean updatedInList;
        try {
            // Pass the updatedUser object to DAO. DAO should find by username and update.
            updatedInList = userDao.updateUser(updatedUser); // DAO updates the file
            System.out.println("User '" + usernameToUpdate + "' updated in users.csv: " + updatedInList);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid user data passed to UserDao for update: " + e.getMessage());
            throw new IllegalArgumentException("Failed to save updated user information. " + e.getMessage(), e);
        }
        catch (IOException e) {
            System.err.println("Error updating user in users.csv: " + e.getMessage());
            throw new IOException("Error updating user in user list file. " + e.getMessage(), e);
        }


        if (updatedInList) {
            // 3. Update in-memory cache
            // Update the object in the cache directly with the fields from updatedUser.
            User userInCache = userCache.get(usernameToUpdate);
            if (userInCache != null) {
                // Apply updates to the object in cache. Only update fields allowed by updateUser method.
                if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                    userInCache.setPassword(updatedUser.getPassword());
                }
                if (updatedUser.getRole() != null && !updatedUser.getRole().isEmpty()) {
                    userInCache.setRole(updatedUser.getRole());
                }
                // File paths are NOT updated via updateUser in DAO currently, so don't update them in cache here.
                System.out.println("User '" + usernameToUpdate + "' updated in cache.");
            } else {
                // If not in cache but updated in file, reload all users cache (safer, though less performant)
                System.out.println("User '" + usernameToUpdate + "' updated in file but not found in cache. Reloading all user cache.");
                loadUsers(); // Reload the entire cache
            }

            return true; // Update in list was successful
        } else {
            System.err.println("Update failed: DAO reported user '" + usernameToUpdate + "' not found during update call.");
            throw new IllegalArgumentException("User '" + usernameToUpdate + "' not found for update.");
        }
    }


    /**
     * Retrieves a user by username directly from the data source file (bypasses cache).
     * Useful for checking existence without relying solely on potentially stale cache.
     * NOTE: This method bypasses the cache. Use getUserByUsername for cached access.
     * @param username The username.
     * @return The User object if found in the data source, null otherwise.
     * @throws IOException If an I/O error occurs.
     */
    private User getUserByUsernameFromDataSource(String username) throws IOException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        // Reload all users from the file to check for existence
        List<User> allUsers = userDao.getAllUsers(); // This bypasses the cache.
        return allUsers.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst()
                .orElse(null);
    }
}