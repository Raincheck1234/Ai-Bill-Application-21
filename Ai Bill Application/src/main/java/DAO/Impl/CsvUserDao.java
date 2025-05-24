package DAO.Impl;

import Constants.ConfigConstants;
import DAO.UserDao;
import model.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CsvUserDao implements UserDao {

    private final String filePath;

    // Define the header for the users CSV
    private static final String[] HEADERS = {"username", "password", "role", "transaction_csv_path", "summary_csv_path"};

    public CsvUserDao(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<User> getAllUsers() throws IOException {
        List<User> users = new ArrayList<>();
        // Use BOMInputStream to handle potential Byte Order Mark issues
        try (Reader reader = new InputStreamReader(
                new BOMInputStream(Files.newInputStream(Paths.get(filePath))),
                StandardCharsets.UTF_8)) {

            // Configure CSVFormat to handle headers
            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase(true) // Ignore header case for robustness
                    .withTrim(true); // Trim leading/trailing whitespace

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                // Check if the required headers are present
                List<String> requiredHeaders = List.of("username", "password", "role", "transaction_csv_path");
                if (!csvParser.getHeaderMap().keySet().containsAll(requiredHeaders)) {
                    throw new IOException("Missing required headers in users CSV file: " + requiredHeaders);
                }

                for (CSVRecord record : csvParser) {
                    // Basic error handling for potentially missing fields in a row
                    String username = record.get("username");
                    String password = record.get("password");
                    String role = record.get("role");
                    String transactionFilePath = record.get("transaction_csv_path");

                    if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || role == null || role.trim().isEmpty() || transactionFilePath == null || transactionFilePath.trim().isEmpty()) {
                        System.err.println("Skipping malformed user record: " + record.toMap());
                        continue; // Skip this row
                    }

                    User user = new User(username.trim(), password.trim(), role.trim(), transactionFilePath.trim());
                    users.add(user);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users from CSV file: " + filePath);
            e.printStackTrace();
            throw e; // Re-throw the exception after logging
        }
        return users;
    }

    /**
     * Adds a new user to the users CSV file.
     * Appends the user record. Writes header if the file is empty.
     * @param user The new user to add.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If user data is invalid.
     */
    @Override // Implement the new interface method
    public void addUser(User user) throws IOException, IllegalArgumentException {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty() ||
                user.getPassword() == null || user.getPassword().trim().isEmpty() ||
                user.getRole() == null || user.getRole().trim().isEmpty() ||
                user.getTransactionFilePath() == null || user.getTransactionFilePath().trim().isEmpty() ||
                user.getSummaryFilePath() == null || user.getSummaryFilePath().trim().isEmpty()) { // Validate all fields
            throw new IllegalArgumentException("Invalid user data: essential fields are null or empty.");
        }


        Path path = Paths.get(filePath);
        // Ensure the directory exists
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        boolean fileExistsAndNotEmpty = Files.exists(path) && Files.size(path) > 0;

        // Define the header for the users CSV
        String[] headers = {"username", "password", "role", "transaction_csv_path", "summary_csv_path"};

        // Use StandardOpenOption.CREATE (creates if not exists) and APPEND
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             // Use CSVPrinter.withHeader ONLY if the file was empty BEFORE opening
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withTrim()
                     .withHeader(fileExistsAndNotEmpty ? new String[0] : headers))) // Write header only if file is new
        {
            // Append the new user record
            csvPrinter.printRecord(
                    user.getUsername(),
                    user.getPassword(), // Store password directly (for simplicity in this project)
                    user.getRole(),
                    user.getTransactionFilePath(),
                    user.getSummaryFilePath()
            );
            // csvPrinter.flush(); // Auto-flushed on close
            System.out.println("Added user '" + user.getUsername() + "' to users CSV: " + filePath);

        } catch (IOException e) {
            System.err.println("Error adding user to CSV file: " + filePath);
            e.printStackTrace();
            throw e; // Re-throw
        }
    }

    /**
     * Deletes a user identified by username from the users CSV file.
     * Also attempts to delete the user's transaction and summary files.
     *
     * @param username The username of the user to delete.
     * @return true if a user was found and deleted, false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
    @Override // Implement the new interface method
    public boolean deleteUser(String username) throws IOException {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Cannot delete user: username is null or empty.");
            return false;
        }
        String usernameToDel = username.trim();
        System.out.println("Attempting to delete user: " + usernameToDel + " from " + filePath);

        // 1. Load all users
        List<User> allUsers = getAllUsers();

        // 2. Filter out the user to be deleted
        List<User> updatedUsers = allUsers.stream()
                .filter(user -> !user.getUsername().trim().equals(usernameToDel))
                .collect(Collectors.toList());

        // Check if a user was actually removed
        boolean userFoundAndRemoved = allUsers.size() > updatedUsers.size();

        if (userFoundAndRemoved) {
            // Find the user object to get file paths before writing updated list
            User userToDelete = allUsers.stream()
                    .filter(user -> user.getUsername().trim().equals(usernameToDel))
                    .findFirst().orElse(null); // Should not be null if userFoundAndRemoved is true

            // 3. Write the remaining users back to the CSV file (Atomic write)
            writeUsersToCSV(filePath, updatedUsers);
            System.out.println("User '" + usernameToDel + "' removed from users CSV.");

            // 4. Attempt to delete associated data files
            if (userToDelete != null) {
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
                    // Decide if user deletion should fail if file deletion fails.
                    // For now, user is deleted from list, just log file deletion error.
                }
            }

            return true; // User successfully deleted from list
        } else {
            System.out.println("User '" + usernameToDel + "' not found in users CSV.");
            return false; // User not found
        }
    }

    /**
     * Updates an existing user's information in the users CSV file.
     * Matches by username.
     *
     * @param updatedUser The User object with updated information (matched by username).
     * @return true if the user was found and updated, false otherwise.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If updatedUser data is invalid or username is missing.
     */
    @Override // Implement the new interface method
    public boolean updateUser(User updatedUser) throws IOException, IllegalArgumentException {
        if (updatedUser == null || updatedUser.getUsername() == null || updatedUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Updated user object and username cannot be null or empty.");
        }
        String usernameToUpdate = updatedUser.getUsername().trim();
        System.out.println("Attempting to update user: " + usernameToUpdate + " in " + filePath);


        // 1. Load all users
        List<User> allUsers = getAllUsers();

        // 2. Find and update the user in the list
        boolean userFoundAndUpdatedInMemory = false;
        List<User> updatedList = new ArrayList<>(allUsers.size()); // Create a new list or modify in place

        for (User user : allUsers) {
            if (user.getUsername().trim().equals(usernameToUpdate)) {
                // Found the user, apply updates from updatedUser object
                // Only update fields that are not null/empty in updatedUser (or based on specific logic)
                // For simplicity, let's apply all fields from updatedUser, except maybe username itself if it's the key.
                // Assuming username is the key and not intended to be changed via this method.
                // If password is empty string in updatedUser, maybe keep old password? Or allow empty password?
                // Let's assume non-null/non-empty fields in updatedUser overwrite. Empty string might mean clear.
                if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                    user.setPassword(updatedUser.getPassword());
                }
                if (updatedUser.getRole() != null && !updatedUser.getRole().isEmpty()) { // Allow setting empty role? Probably not.
                    user.setRole(updatedUser.getRole());
                }
                // File paths usually shouldn't be updated directly via updateUser if they follow a convention.
                // If they can be manually reassigned, add logic here.
                // if (updatedUser.getTransactionFilePath() != null && !updatedUser.getTransactionFilePath().isEmpty()) { ... }
                // if (updatedUser.getSummaryFilePath() != null && !updatedUser.getSummaryFilePath().isEmpty()) { ... }

                System.out.println("User '" + usernameToUpdate + "' found and updated in memory.");
                userFoundAndUpdatedInMemory = true;
            }
            updatedList.add(user); // Add the original or modified user to the new list
        }


        if (userFoundAndUpdatedInMemory) {
            // 3. Write the updated list back to the CSV file (Atomic write)
            writeUsersToCSV(filePath, updatedList);
            System.out.println("User '" + usernameToUpdate + "' updated and users CSV written.");
        } else {
            System.out.println("User '" + usernameToUpdate + "' not found for update in users CSV.");
        }

        return userFoundAndUpdatedInMemory; // Return true if user was found and updated
    }

    /**
     * Writes a list of users to the users CSV file, overwriting existing data (Atomic write).
     *
     * @param filePath The path to the users CSV file.
     * @param users The list of users to write.
     * @throws IOException If an I/O error occurs.
     */
    private void writeUsersToCSV(String filePath, List<User> users) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        File targetFile = path.toFile();
        File tempFile = File.createTempFile("users_temp_", ".csv", targetFile.getParentFile());

        // Use the defined HEADERS for writing
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(HEADERS).withTrim())) {

            for (User user : users) {
                csvPrinter.printRecord(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole(),
                        user.getTransactionFilePath(),
                        user.getSummaryFilePath() // Include the new field
                );
            }
        } catch (IOException e) {
            if (tempFile.exists()) tempFile.delete(); // Clean up temp file on failure
            System.err.println("Error writing users to temporary CSV file: " + tempFile.toPath());
            e.printStackTrace();
            throw e;
        }

        // Atomic replacement
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Atomically replaced " + filePath + " with updated users data.");
        } catch (IOException e) {
            System.err.println("Failed to atomically replace original users file: " + targetFile.toPath());
            if (tempFile.exists()) tempFile.delete();
            e.printStackTrace();
            throw e;
        }
    }

    // Helper method to parse a single record (optional, can be in getAllUsers)
    // Helper method to parse a single CSV record into a User object (same as before)
    // Helper method to parse a single CSV record into a User object (same as before)
    private User parseRecord(CSVRecord record) {
        String username = record.get("username");
        String password = record.get("password");
        String role = record.get("role");
        String transactionFilePath = record.get("transaction_csv_path");
        String summaryFilePath = record.get("summary_csv_path");


        // Basic validation for essential fields (should match headers)
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                role == null || role.trim().isEmpty() ||
                transactionFilePath == null || transactionFilePath.trim().isEmpty() ||
                summaryFilePath == null || summaryFilePath.trim().isEmpty())
        {
            System.err.println("Skipping malformed user record due to missing essential fields: " + record.toMap());
            return null; // Indicate parsing failed for this record
        }


        return new User(
                username.trim(),
                password.trim(),
                role.trim(),
                transactionFilePath.trim(),
                summaryFilePath.trim()
        );
    }

}