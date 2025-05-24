package Service.Impl;

import Constants.StandardCategories;
import DAO.TransactionDao; // Import the interface
import DAO.Impl.CsvTransactionDao; // Import the implementation
import Service.TransactionService;
import Utils.CacheManager; // Import the new CacheManager
import model.MonthlySummary;
import model.Transaction;

import javax.swing.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // Added for clarity in catch blocks if specific parsing errors are handled
import java.util.*;
import java.util.stream.Collectors; // Needed for search

public class TransactionServiceImpl implements TransactionService {

    private final String currentUserTransactionFilePath; // Store the user's file path
    // TransactionDao instance needed to load data if cache misses
    private final TransactionDao transactionDao;

    /**
     * Constructor now accepts the user's transaction file path.
     *
     * @param currentUserTransactionFilePath The file path for the current user's transactions.
     */
    public TransactionServiceImpl(String currentUserTransactionFilePath) {
        this.currentUserTransactionFilePath = currentUserTransactionFilePath;
        // Create a DAO instance for this service instance.
        this.transactionDao = new CsvTransactionDao(); // One DAO instance per service instance
        System.out.println("TransactionServiceImpl initialized for file: " + currentUserTransactionFilePath);
        // Cache is managed by CacheManager, not directly by this instance.
    }

    @Override // Implement the new interface method
    public List<Transaction> getAllTransactions() throws Exception {
        // Simply call the internal method that uses the cache
        return getAllTransactionsForCurrentUser();
    }

    /**
     * Imports transactions from a given CSV file path into the current user's transactions.
     * Reads the import file, merges with existing data, and saves back.
     *
     * @param userFilePath The file path for the current user's transactions (target).
     * @param importFilePath The file path of the CSV to import from (source).
     * @return The number of transactions successfully imported.
     * @throws Exception If an error occurs during reading, parsing, or saving.
     */
    @Override // Implement the new interface method
    public int importTransactionsFromCsv(String userFilePath, String importFilePath) throws Exception {
        System.out.println("Starting import from " + importFilePath + " to user file " + userFilePath);
        List<Transaction> existingTransactions;
        List<Transaction> transactionsToImport;

        try {
            // 1. Load existing transactions for the current user (from cache/file)
            // Use the method that uses the CacheManager
            existingTransactions = getAllTransactions(); // Already uses CacheManager

            // 2. Read and parse transactions from the import file
            // Use the DAO's loadFromCSV method with the import file path
            // Need a *separate* DAO instance or method call that targets the import file
            TransactionDao importDao = new CsvTransactionDao(); // Create a temporary DAO for reading the import file
            transactionsToImport = importDao.loadFromCSV(importFilePath); // Load from the selected file
            System.out.println("Read " + transactionsToImport.size() + " transactions from import file.");

        } catch (IOException e) {
            System.err.println("Error loading files during import process.");
            e.printStackTrace();
            throw new Exception("Failed to read transaction data!", e); // Wrap and re-throw
        }

        // 3. Merge imported transactions with existing ones
        // Simple merge: add all imported transactions.
        // Handle potential duplicates: check if order number exists.
        // If order numbers are not guaranteed unique in imported file or against existing,
        // consider generating new unique IDs for imported items if their ON is empty or conflicts.
        List<Transaction> mergedTransactions = new ArrayList<>(existingTransactions);
        int importedCount = 0;

        for (Transaction importedTx : transactionsToImport) {
            // Basic Check: Ensure imported transaction has an order number or generate one
            if (importedTx.getOrderNumber() == null || importedTx.getOrderNumber().trim().isEmpty()) {
                // Generate a unique ID for transactions without one
                String uniqueId = "IMPORT_" + UUID.randomUUID().toString();
                importedTx.setOrderNumber(uniqueId);
                System.out.println("Generated unique order number for imported transaction: " + uniqueId);
            } else {
                // Check for potential duplicate order number against existing transactions
                boolean duplicate = existingTransactions.stream()
                        .anyMatch(t -> t.getOrderNumber().trim().equals(importedTx.getOrderNumber().trim()));
                if (duplicate) {
                    System.err.println("Skipping imported transaction due to duplicate order number: " + importedTx.getOrderNumber());
                    // Decide: skip, overwrite, or generate new ID. Skipping for now.
                    JOptionPane.showMessageDialog(null, "Duplicate transaction order number found: " + importedTx.getOrderNumber() + ", skipped.", "Import Warning", JOptionPane.WARNING_MESSAGE);
                    continue; // Skip this duplicate transaction
                }
            }

            // Add the transaction to the merged list
            mergedTransactions.add(importedTx);
            importedCount++;
        }
        System.out.println("Merged transactions. Total after merge: " + mergedTransactions.size() + ". Successfully imported count: " + importedCount);


        // 4. Save the merged list back to the current user's file
        try {
            // Use the DAO instance associated with this service
            transactionDao.writeTransactionsToCSV(userFilePath, mergedTransactions);
            System.out.println("Saved merged transactions to user file: " + userFilePath);

            // 5. Invalidate or update the cache for the current user's file
            // Invalidation is simpler: forces CacheManager to reload from the updated file next time.
            CacheManager.invalidateTransactionCache(userFilePath);
            System.out.println("Cache invalidated for user file: " + userFilePath);


        } catch (IOException e) {
            System.err.println("Error saving merged transactions after import.");
            e.printStackTrace();
            // Consider leaving the original file untouched on save failure
            throw new Exception("Failed to save imported transaction data!", e); // Wrap and re-throw
        }

        System.out.println("Import process finished.");
        return importedCount; // Return the count of transactions actually added
    }

    /**
     * Gets all transactions for the current user from the cache (loading if necessary).
     * @return List of transactions.
     * @throws Exception If an error occurs during loading.
     */
    private List<Transaction> getAllTransactionsForCurrentUser() throws Exception {
        // Get transactions using the CacheManager for the current user's file
        return CacheManager.getTransactions(currentUserTransactionFilePath, transactionDao);
    }

    /**
     * Add transaction for the current user.
     *
     * @param transaction The new transaction to add.
     */
    @Override
    public void addTransaction(Transaction transaction) throws IOException {
        // Set transaction time to current time if not already set
        if (transaction.getTransactionTime() == null || transaction.getTransactionTime().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            String currentTime = now.format(formatter);
            transaction.setTransactionTime(currentTime);
        }

        try {
            // Call DAO layer to add transaction to the user's specific file
            transactionDao.addTransaction(currentUserTransactionFilePath, transaction);

            // After adding, invalidate the cache for this user's file
            CacheManager.invalidateTransactionCache(currentUserTransactionFilePath);
            System.out.println("Transaction added and cache invalidated for " + currentUserTransactionFilePath);

        } catch (IOException e) {
            System.err.println("Error adding transaction for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            throw e; // Re-throw
        }
    }

    /**
     * Change transaction information for the current user.
     *
     * @param updatedTransaction The transaction object with updated information.
     */
    @Override
    public void changeTransaction(Transaction updatedTransaction) throws Exception {
        try {
            // Load existing transactions (from cache/file)
            List<Transaction> allTransactions = getAllTransactionsForCurrentUser();

            // Find and update the target transaction in the list
            boolean foundAndUpdatedInMemory = false;
            List<Transaction> updatedList = new ArrayList<>(allTransactions.size());
            for (Transaction t : allTransactions) {
                if (t.getOrderNumber().trim().equals(updatedTransaction.getOrderNumber().trim())) {
                    // Found the transaction, apply updates
                    updateTransactionFields(t, updatedTransaction); // Helper method to apply updates
                    updatedList.add(t); // Add the modified transaction
                    foundAndUpdatedInMemory = true;
                    System.out.println("Transaction with order number " + updatedTransaction.getOrderNumber() + " found and updated in memory.");
                } else {
                    updatedList.add(t); // Add unchanged transactions
                }
            }

            if (!foundAndUpdatedInMemory) {
                throw new IllegalArgumentException("Transaction order number not found: " + updatedTransaction.getOrderNumber() + " in file " + currentUserTransactionFilePath);
            }

            // Write the entire updated list back to the CSV file
            transactionDao.writeTransactionsToCSV(currentUserTransactionFilePath, updatedList);
            System.out.println("Updated transaction with order number " + updatedTransaction.getOrderNumber() + " and wrote back to file.");

            // Update the cache with the modified list
            CacheManager.putTransactions(currentUserTransactionFilePath, updatedList, transactionDao);
            System.out.println("Cache updated with the modified transaction list for " + currentUserTransactionFilePath);

        } catch (IOException e) {
            System.err.println("Error changing transaction for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            throw e;
        } catch (Exception e) { // Catch exception from getAllTransactionsForCurrentUser
            System.err.println("Error loading transactions for change operation: " + currentUserTransactionFilePath);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Helper method: Updates non-empty fields from source to target.
     */
    private void updateTransactionFields(Transaction target, Transaction source) {
        // OrderNumber is the key and should generally not be updated this way.
        if (source.getTransactionTime() != null && !source.getTransactionTime().trim().isEmpty()) {
            target.setTransactionTime(source.getTransactionTime().trim());
        }
        if (source.getTransactionType() != null && !source.getTransactionType().trim().isEmpty()) {
            target.setTransactionType(source.getTransactionType().trim());
        }
        if (source.getCounterparty() != null && !source.getCounterparty().trim().isEmpty()) {
            target.setCounterparty(source.getCounterparty().trim());
        }
        if (source.getCommodity() != null && !source.getCommodity().trim().isEmpty()) {
            target.setCommodity(source.getCommodity().trim());
        }
        if (source.getInOut() != null && !source.getInOut().trim().isEmpty()) {
            String inOut = source.getInOut().trim();
            // Assuming inOut values from UI are already validated or are "Income"/"Expense" or "In"/"Out"
            if (inOut.equalsIgnoreCase("Income") || inOut.equalsIgnoreCase("In") ||
                    inOut.equalsIgnoreCase("Expense") || inOut.equalsIgnoreCase("Out")) {
                target.setInOut(inOut);
            } else {
                System.err.println("Warning: Invalid value for In/Out: " + source.getInOut() + ". Keeping original.");
            }
        }
        // For paymentAmount, assume the value from the source (e.g., UI dialog) is the intended new value.
        target.setPaymentAmount(source.getPaymentAmount());

        if (source.getPaymentMethod() != null && !source.getPaymentMethod().trim().isEmpty()) {
            target.setPaymentMethod(source.getPaymentMethod().trim());
        }
        if (source.getCurrentStatus() != null && !source.getCurrentStatus().trim().isEmpty()) {
            target.setCurrentStatus(source.getCurrentStatus().trim());
        }
        if (source.getMerchantNumber() != null && !source.getMerchantNumber().trim().isEmpty()) {
            target.setMerchantNumber(source.getMerchantNumber().trim());
        }
        if (source.getRemarks() != null && !source.getRemarks().trim().isEmpty()) { // Remarks can be empty, so allow empty string.
            target.setRemarks(source.getRemarks().trim());
        }
        System.out.println("Applied updates to transaction: " + target.getOrderNumber());
    }


    /**
     * Delete transaction for the current user by order number.
     *
     * @param orderNumber The unique order number of the transaction to delete.
     * @return true if deletion was successful.
     * @throws Exception If an error occurs or transaction is not found.
     */
    @Override
    public boolean deleteTransaction(String orderNumber) throws Exception {
        try {
            // Call DAO layer to delete transaction from the user's specific file
            boolean deleted = transactionDao.deleteTransaction(currentUserTransactionFilePath, orderNumber);

            if (deleted) {
                // After deleting, invalidate the cache for this user's file
                CacheManager.invalidateTransactionCache(currentUserTransactionFilePath);
                System.out.println("Transaction with order number " + orderNumber + " deleted and cache invalidated for " + currentUserTransactionFilePath);
            } else {
                System.out.println("Transaction with order number " + orderNumber + " not found for deletion in " + currentUserTransactionFilePath);
            }
            return deleted;

        } catch (IOException e) {
            System.err.println("Error deleting transaction for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Search transactions for the current user based on criteria.
     *
     * @param searchCriteria The Transaction object containing search criteria.
     * @return List of matched transactions.
     */
    @Override
    public List<Transaction> searchTransaction(Transaction searchCriteria) {
        try {
            List<Transaction> allTransactions = getAllTransactionsForCurrentUser();
            System.out.println("Searching through " + allTransactions.size() + " transactions for user " + currentUserTransactionFilePath);

            List<Transaction> matched = allTransactions.stream()
                    .filter(t -> matchesCriteria(t, searchCriteria))
                    .collect(Collectors.toList());
            System.out.println("Found " + matched.size() + " matching transactions.");

            matched.sort((t1, t2) -> {
                LocalDateTime time1 = parseDateTimeSafe(t1.getTransactionTime());
                LocalDateTime time2 = parseDateTimeSafe(t2.getTransactionTime());
                if (time1 != null && time2 != null) {
                    return time2.compareTo(time1); // Newest first
                } else if (time1 == null && time2 == null) return 0;
                else if (time1 == null) return 1;
                else return -1;
            });
            System.out.println("Matched transactions sorted.");
            return matched;
        } catch (Exception e) {
            System.err.println("Error during search operation for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Helper method: Checks if a single transaction matches the search criteria.
     */
    private boolean matchesCriteria(Transaction transaction, Transaction criteria) {
        return containsIgnoreCase(transaction.getTransactionTime(), criteria.getTransactionTime())
                && containsIgnoreCase(transaction.getTransactionType(), criteria.getTransactionType())
                && containsIgnoreCase(transaction.getCounterparty(), criteria.getCounterparty())
                && containsIgnoreCase(transaction.getCommodity(), criteria.getCommodity())
                && matchesInOutCriteria(transaction.getInOut(), criteria.getInOut())
                && containsIgnoreCase(transaction.getPaymentMethod(), criteria.getPaymentMethod());
    }

    /**
     * Helper method: Fuzzy match string, ignoring case and trimming whitespace.
     * An empty/null target criteria matches everything.
     */
    private boolean containsIgnoreCase(String source, String target) {
        if (target == null || target.trim().isEmpty()) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return source.trim().toLowerCase().contains(target.trim().toLowerCase());
    }

    /**
     * Helper method: Matches In/Out criteria. Handles "Income" vs "In", "Expense" vs "Out".
     * An empty/null target criteria matches everything.
     */
    private boolean matchesInOutCriteria(String source, String target) {
        if (target == null || target.trim().isEmpty()) {
            return true;
        }
        if (source == null) {
            return false;
        }
        String sourceTrimmed = source.trim();
        String targetTrimmed = target.trim();

        if (targetTrimmed.equalsIgnoreCase("Income") || targetTrimmed.equalsIgnoreCase("In")) {
            return sourceTrimmed.equalsIgnoreCase("Income") || sourceTrimmed.equalsIgnoreCase("In");
        }
        if (targetTrimmed.equalsIgnoreCase("Expense") || targetTrimmed.equalsIgnoreCase("Out")) {
            return sourceTrimmed.equalsIgnoreCase("Expense") || sourceTrimmed.equalsIgnoreCase("Out");
        }
        return sourceTrimmed.toLowerCase().contains(targetTrimmed.toLowerCase());
    }

    /**
     * Helper method: Safely parses a time string into LocalDateTime.
     * Returns null if parsing fails.
     */
    private LocalDateTime parseDateTimeSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        timeStr = timeStr.trim().replaceAll("\\s+", " ");

        if (timeStr.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            timeStr += " 00:00";
        } else if (timeStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            timeStr += " 00:00:00";
        }

        List<String> patterns = List.of(
                "yyyy/M/d H:mm", "yyyy/M/d HH:mm",
                "yyyy/MM/d H:mm", "yyyy/MM/d HH:mm",
                "yyyy/M/dd H:mm", "yyyy/M/dd HH:mm",
                "yyyy/MM/dd H:mm", "yyyy/MM/dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        );
        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }
        System.err.println("Failed to parse date string: " + timeStr);
        return null;
    }

    /**
     * Aggregates transactions for the current user by month and standard category.
     *
     * @return A map where keys are month identifiers (e.g., "YYYY-MM") and values are MonthlySummary objects.
     * @throws Exception If an error occurs during data retrieval.
     */
    @Override
    public Map<String, MonthlySummary> getMonthlyTransactionSummary() throws Exception {
        System.out.println("Generating monthly transaction summary for user file: " + currentUserTransactionFilePath);
        List<Transaction> allTransactions;
        try {
            allTransactions = getAllTransactions();
            System.out.println("Retrieved " + allTransactions.size() + " transactions for summary.");
        } catch (Exception e) {
            System.err.println("Error retrieving transactions for summary generation.");
            e.printStackTrace();
            throw new Exception("Failed to get transaction data!", e);
        }

        Map<String, MonthlySummary> monthlySummaries = new HashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Transaction t : allTransactions) {
            if (t.getTransactionTime() == null || t.getTransactionTime().trim().isEmpty()) {
                System.err.println("Skipping transaction with no time for summary aggregation: " + t.getOrderNumber());
                continue;
            }
            LocalDate date = parseDateFromTransactionTimeSafe(t.getTransactionTime());
            if (date == null) {
                System.err.println("Skipping transaction with unparseable date for summary aggregation: " + t.getTransactionTime() + " - " + t.getOrderNumber());
                continue;
            }
            String monthIdentifier = YearMonth.from(date).format(monthFormatter);
            monthlySummaries.putIfAbsent(monthIdentifier, new MonthlySummary(monthIdentifier));
            MonthlySummary currentMonthSummary = monthlySummaries.get(monthIdentifier);

            if (t.getInOut() != null) {
                String inOut = t.getInOut().trim();
                if (inOut.equalsIgnoreCase("Income") || inOut.equalsIgnoreCase("In")) { // Use English here
                    currentMonthSummary.addIncome(t.getPaymentAmount());
                } else if (inOut.equalsIgnoreCase("Expense") || inOut.equalsIgnoreCase("Out")) { // Use English here
                    String rawType = t.getTransactionType();
                    String standardCategory = StandardCategories.getStandardCategory(rawType);
                    String effectiveExpenseCategoryForSummary = StandardCategories.isStandardExpenseCategory(standardCategory) ? standardCategory : StandardCategories.EXPENSE_CATEGORIES.get(StandardCategories.EXPENSE_CATEGORIES.size() - 1); // Default to "Other Expenses"
                    currentMonthSummary.addExpense(t.getPaymentAmount(), effectiveExpenseCategoryForSummary);
                }
            }
        }
        System.out.println("Generated summary for " + monthlySummaries.size() + " months.");
        return monthlySummaries;
    }

    /**
     * Helper method to parse date from transaction time string safely.
     */
    private LocalDate parseDateFromTransactionTimeSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String datePart = timeStr.split(" ")[0];
        datePart = datePart.trim().replace('-', '/');

        List<String> patterns = List.of(
                "yyyy/M/d", "yyyy/MM/d", "yyyy/M/dd", "yyyy/MM/dd"
        );
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(datePart, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }
        System.err.println("TransactionServiceImpl: Failed to parse date part '" + datePart + "' from transaction time: " + timeStr);
        return null;
    }
}