package DAO.Impl;

import Constants.ConfigConstants;
import DAO.UserDao;
import model.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CsvUserDao implements UserDao {

    private final String filePath;

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

    // Helper method to parse a single record (optional, can be in getAllUsers)
    // Helper method to parse a single CSV record into a User object (same as before)
    private User parseRecord(CSVRecord record) {
        // ... existing implementation ...
        String username = record.get("username");
        String password = record.get("password");
        String role = record.get("role");
        String transactionFilePath = record.get("transaction_csv_path");
        String summaryFilePath = record.get("summary_csv_path"); // Read the new field

        // Basic validation for essential fields
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                role == null || role.trim().isEmpty() ||
                transactionFilePath == null || transactionFilePath.trim().isEmpty() ||
                summaryFilePath == null || summaryFilePath.trim().isEmpty()) // Validate new field too
        {
            System.err.println("Skipping malformed user record due to missing essential fields: " + record.toMap());
            return null;
        }


        return new User(
                username.trim(),
                password.trim(),
                role.trim(),
                transactionFilePath.trim(),
                summaryFilePath.trim() // Include in User object creation
        );
    }

}