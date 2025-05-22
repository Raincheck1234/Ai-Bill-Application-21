package DAO.Impl;

import Constants.ConfigConstants;
import DAO.UserDao;
import model.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    // Helper method to parse a single record (optional, can be in getAllUsers)
    // private User parseRecord(CSVRecord record) { ... }
}