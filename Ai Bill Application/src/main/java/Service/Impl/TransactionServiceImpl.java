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
import java.util.*;
import java.util.stream.Collectors; // Needed for search

// Remove static field
// public static CsvTransactionDao csvTransactionDao;

// Remove direct CacheUtil instance
// public final CacheUtil<String, List<Transaction>, Exception> cache;

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
            throw new Exception("读取交易数据失败！", e); // Wrap and re-throw
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
                    JOptionPane.showMessageDialog(null, "发现重复交易单号: " + importedTx.getOrderNumber() + ", 已跳过。", "导入警告", JOptionPane.WARNING_MESSAGE);
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
            // Use the DAO instance associated with this service (which knows the user's file implicitly via CacheManager interactions, but writeAllStatistics needs the path explicitly)
            // The transactionDao field is initialized as CsvTransactionDao, which has writeAllStatistics.
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
            throw new Exception("保存导入的交易数据失败！", e); // Wrap and re-throw
        }

        System.out.println("Import process finished.");
        return importedCount; // Return the count of transactions actually added
    }

    /**
     * Gets all transactions for the current user from the cache (loading if necessary).
     * Made protected or public if needed by subclasses, but private is fine for now.
     * @return List of transactions.
     * @throws Exception If an error occurs during loading.
     */
    private List<Transaction> getAllTransactionsForCurrentUser() throws Exception { // Kept as private or change if needed
        // Get transactions using the CacheManager for the current user's file
        return CacheManager.getTransactions(currentUserTransactionFilePath, transactionDao);
    }


//    /**
//     * Gets all transactions for the current user from the cache (loading if necessary).
//     *
//     * @return List of transactions.
//     * @throws Exception If an error occurs during loading.
//     */
//    private List<Transaction> getAllTransactionsForCurrentUser() throws Exception {
//        // Get transactions using the CacheManager for the current user's file
//        return CacheManager.getTransactions(currentUserTransactionFilePath, transactionDao);
//    }


    /**
     * Add transaction for the current user.
     *
     * @param transaction The new transaction to add.
     */
    @Override
    public void addTransaction(Transaction transaction) throws IOException {
        // 增强参数验证
        if (transaction == null) {
            throw new IllegalArgumentException("交易记录不能为空");
        }
        
        // 严格验证交易时间格式
        String transactionTime = transaction.getTransactionTime();
        if (transactionTime != null && !transactionTime.isEmpty()) {
            if (!transactionTime.matches("^\\d{4}/(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01]) ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")) {
                throw new IllegalArgumentException("交易时间格式必须为 yyyy/MM/dd HH:mm:ss（例: 2023/12/31 23:59:59）");
            }
        } else {
            // 自动生成符合格式的当前时间
            transaction.setTransactionTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        }

        // 验证金额为有效数字
        if (transaction.getPaymentAmount() <= 0) {
            throw new IllegalArgumentException("交易金额必须为大于零的有效数字");
        }

        // 严格验证交易状态
        String status = transaction.getCurrentStatus();
        if (status == null || !status.matches("^(已完成|未完成)$")) {
            throw new IllegalArgumentException("当前状态必须为'已完成'或'未完成'");
        }

        // 保留原有交易类型验证
        if (transaction.getTransactionType() == null || transaction.getTransactionType().trim().isEmpty()) {
            throw new IllegalArgumentException("交易类型不能为空");
        }

        try {
            // Call DAO layer to add transaction to the user's specific file
            transactionDao.addTransaction(currentUserTransactionFilePath, transaction);

            // After adding, invalidate the cache for this user's file
            // Or, ideally, reload the data and put the new list into the cache.
            // Invalidation is simpler for now, forcing a reload on next get.
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
            List<Transaction> updatedList = new ArrayList<>(allTransactions.size()); // Create a new list or modify in place
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
                throw new IllegalArgumentException("未找到交易单号: " + updatedTransaction.getOrderNumber() + " 在文件 " + currentUserTransactionFilePath + " 中");
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
        // 增强字段验证
        if (source.getTransactionTime() != null && !source.getTransactionTime().trim().isEmpty()) {
            String newTime = source.getTransactionTime().trim();
            if (!newTime.matches("^\\d{4}/(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01]) ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")) {
                throw new IllegalArgumentException("更新时间格式必须为 yyyy/MM/dd HH:mm:ss（例: 2023/12/31 23:59:59）");
            }
            target.setTransactionTime(newTime);
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
        // Handle InOut specifically if it's from a ComboBox with predefined options
        if (source.getInOut() != null && !source.getInOut().trim().isEmpty()) {
            String inOut = source.getInOut().trim();
            if (inOut.equals("收入") || inOut.equals("支出") || inOut.equals("支") || inOut.equals("收")) { // Be flexible with input
                target.setInOut(inOut);
            } else {
                System.err.println("Warning: Invalid value for 收/支: " + source.getInOut() + ". Keeping original.");
                // Optionally throw an IllegalArgumentException
            }
        }
        // Handle paymentAmount - 0.0 might be a valid amount, check if it was explicitly set
        // A better approach for primitive types is to check if the source object
        // represents a "partial update" and how unset primitives are marked.
        // For simplicity here, let's assume 0.0 *is* a valid amount that can be set.
        // If you need to differentiate "not set" from "set to 0.0", the source object
        // would need flags or use wrapper types (Double) and check for null.
        // Let's refine this: Only update if the source amount is NOT 0.0, or if the source object signals it's a full update.
        // Assuming the UI passes a new Transaction object where primitive 0.0 means 'not updated'.
        // This is a common pattern but needs careful handling.
        // If the UI explicitly allows setting 0.0, this logic needs adjustment.
        // For now, let's assume 0.0 is treated as 'no update' UNLESS the original transaction amount was also 0.0.
        // A safer way: If the user edited the amount field in the dialog, we *should* update it, even to 0.0.
        // The MenuUI's editRow extracts values into fields, so we can assume the value from fields[5].getText()
        // represents the user's intended new value. The Double.parseDouble already happened in MenuUI.
        // So, if the source object has a non-zero amount, update. What if the user wants to set it to 0?
        // The current dialog doesn't distinguish. Let's assume for now that any double value from the dialog
        // should be applied. This might need refinement based on UI behavior.
        target.setPaymentAmount(source.getPaymentAmount()); // Simply update the amount


        if (source.getPaymentMethod() != null && !source.getPaymentMethod().trim().isEmpty()) {
            target.setPaymentMethod(source.getPaymentMethod().trim());
        }
        if (source.getCurrentStatus() != null && !source.getCurrentStatus().trim().isEmpty()) {
            String newStatus = source.getCurrentStatus().trim();
            if (!newStatus.matches("^(已完成|未完成)$")) {
                throw new IllegalArgumentException("当前状态必须为'已完成'或'未完成'");
            }
            target.setCurrentStatus(newStatus);
        }
        // OrderNumber is typically the key, updating it is risky and often disallowed.
        // If allowed, need to ensure uniqueness and handle file operations carefully.
        // Let's assume OrderNumber should NOT be changed via this method.
        // if (source.getOrderNumber() != null && !source.getOrderNumber().trim().isEmpty()) {
        //     target.setOrderNumber(source.getOrderNumber().trim()); // Potential issue if new ON conflicts
        // }
        if (source.getMerchantNumber() != null && !source.getMerchantNumber().trim().isEmpty()) {
            target.setMerchantNumber(source.getMerchantNumber().trim());
        }
        if (source.getRemarks() != null && !source.getRemarks().trim().isEmpty()) {
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
                // If DAO returns false, it means the order number was not found.
                System.out.println("Transaction with order number " + orderNumber + " not found for deletion in " + currentUserTransactionFilePath);
            }
            return deleted; // Return true if deletion occurred, false if not found

        } catch (IOException e) {
            System.err.println("Error deleting transaction for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            throw e; // Re-throw
        }
        // No need for explicit "未找到交易单号" exception here if DAO returns false,
        // MenuUI can check the boolean result and show a message.
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
            // 1. Get all transactions for the current user (from cache/file)
            List<Transaction> allTransactions = getAllTransactionsForCurrentUser();
            System.out.println("Searching through " + allTransactions.size() + " transactions for user " + currentUserTransactionFilePath);


            // 2. Filter transactions based on criteria
            // Use stream().filter() for conciseness and potential parallelism (though unlikely needed here)
            List<Transaction> matched = allTransactions.stream()
                    .filter(t -> matchesCriteria(t, searchCriteria))
                    .collect(Collectors.toList());
            System.out.println("Found " + matched.size() + " matching transactions.");


            // 3. Sort matched transactions by time, newest first
            matched.sort((t1, t2) -> {
                // Safely parse and compare dates, fall back to string comparison if parsing fails
                LocalDateTime time1 = parseDateTimeSafe(t1.getTransactionTime());
                LocalDateTime time2 = parseDateTimeSafe(t2.getTransactionTime());

                if (time1 != null && time2 != null) {
                    return time2.compareTo(time1); // Newest first
                } else if (time1 == null && time2 == null) {
                    return 0; // Both unparseable, treat as equal
                } else if (time1 == null) {
                    return 1; // Unparseable times come later
                } else { // time2 == null
                    return -1; // Unparseable times come later
                }
            });
            System.out.println("Matched transactions sorted.");

            return matched;
        } catch (Exception e) { // Catch exception from getAllTransactionsForCurrentUser
            System.err.println("Error during search operation for user file: " + currentUserTransactionFilePath);
            e.printStackTrace();
            // Depending on UI, you might want to return an empty list or propagate the exception
            // For search, returning empty list and logging error is often user-friendly.
            return List.of();
        }
    }

    /**
     * Helper method: Checks if a single transaction matches the search criteria.
     */
    private boolean matchesCriteria(Transaction transaction, Transaction criteria) {
        // Criteria fields are implicitly ANDed. Null/empty criteria fields match everything.
        return containsIgnoreCase(transaction.getTransactionTime(), criteria.getTransactionTime())
                && containsIgnoreCase(transaction.getTransactionType(), criteria.getTransactionType())
                && containsIgnoreCase(transaction.getCounterparty(), criteria.getCounterparty())
                && containsIgnoreCase(transaction.getCommodity(), criteria.getCommodity())
                && matchesInOutCriteria(transaction.getInOut(), criteria.getInOut()) // Specific check for In/Out
                && containsIgnoreCase(transaction.getPaymentMethod(), criteria.getPaymentMethod());
        // Note: paymentAmount is not used as a search criterion in MenuUI's search panel currently.
        // If needed, add logic here, e.g., checking if criteria.getPaymentAmount() is set
        // and if transaction.getPaymentAmount() falls within a range or matches exactly.
    }

    /**
     * Helper method: Fuzzy match string, ignoring case and trimming whitespace.
     * An empty/null target criteria matches everything.
     */
    private boolean containsIgnoreCase(String source, String target) {
        if (target == null || target.trim().isEmpty()) {
            return true; // Empty criteria matches everything
        }
        if (source == null) {
            return false; // Source is null, cannot contain non-empty target
        }
        return source.trim().toLowerCase().contains(target.trim().toLowerCase());
    }

    /**
     * Helper method: Matches In/Out criteria. Handles cases like "收入" vs "收", "支出" vs "支".
     * An empty/null target criteria matches everything.
     */
    private boolean matchesInOutCriteria(String source, String target) {
        if (target == null || target.trim().isEmpty()) {
            return true; // Empty criteria matches everything
        }
        if (source == null) {
            return false; // Source is null
        }
        String sourceTrimmed = source.trim();
        String targetTrimmed = target.trim();

        if (targetTrimmed.equalsIgnoreCase("收入") || targetTrimmed.equalsIgnoreCase("收")) {
            return sourceTrimmed.equalsIgnoreCase("收入") || sourceTrimmed.equalsIgnoreCase("收");
        }
        if (targetTrimmed.equalsIgnoreCase("支出") || targetTrimmed.equalsIgnoreCase("支")) {
            return sourceTrimmed.equalsIgnoreCase("支出") || sourceTrimmed.equalsIgnoreCase("支");
        }
        // If target is something else, do a simple contains check
        return sourceTrimmed.toLowerCase().contains(targetTrimmed.toLowerCase());
    }


    /**
     * Helper method: Safely parses a time string into LocalDateTime.
     * Returns null if parsing fails.
     * Should match the formats used in AITransactionService.parseDateTime.
     */
    private LocalDateTime parseDateTimeSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // 中文空格等统一清理
        timeStr = timeStr.trim().replaceAll("\\s+", " ");

        // If only date is present, append 00:00
        if (timeStr.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            timeStr += " 00:00";
        } else if (timeStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            timeStr += " 00:00:00"; // Assuming yyyy-MM-dd uses seconds format
        }


        // Try parsing with multiple formats
        List<String> patterns = List.of(
                "yyyy/M/d H:mm", "yyyy/M/d HH:mm",
                "yyyy/MM/d H:mm", "yyyy/MM/d HH:mm",
                "yyyy/M/dd H:mm", "yyyy/M/dd HH:mm",
                "yyyy/MM/dd H:mm", "yyyy/MM/dd HH:mm",
                "yyyy/MM/dd HH:mm:ss", // Added seconds format
                "yyyy-MM-dd HH:mm:ss", // Added dash format
                "yyyy/MM/dd" // Added date only format (already handled by adding 00:00)
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception ignored) {
                // Ignore parsing errors for this pattern and try the next
            }
        }
        System.err.println("Failed to parse date string: " + timeStr);
        return null; // Return null if no pattern matches
    }

    /**
     * Aggregates transactions for the current user by month and standard category.
     *
     * @return A map where keys are month identifiers (e.g., "YYYY-MM") and values are MonthlySummary objects.
     * @throws Exception If an error occurs during data retrieval.
     */
    @Override // Implement the new interface method
    public Map<String, MonthlySummary> getMonthlyTransactionSummary() throws Exception {
        System.out.println("Generating monthly transaction summary for user file: " + currentUserTransactionFilePath);
        List<Transaction> allTransactions;
        try {
            // 1. Get all transactions for the current user (from cache/file)
            allTransactions = getAllTransactions(); // Uses CacheManager
            System.out.println("Retrieved " + allTransactions.size() + " transactions for summary.");

        } catch (Exception e) {
            System.err.println("Error retrieving transactions for summary generation.");
            e.printStackTrace();
            throw new Exception("获取交易数据失败！", e);
        }

        // 2. Aggregate transactions by month and category
        Map<String, MonthlySummary> monthlySummaries = new HashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM"); // Format for month identifier


        for (Transaction t : allTransactions) {
            if (t.getTransactionTime() == null || t.getTransactionTime().trim().isEmpty()) {
                System.err.println("Skipping transaction with no time for summary aggregation: " + t.getOrderNumber());
                continue; // Skip transactions with no time
            }

            // Safely parse the transaction date to get the month
            LocalDate date = parseDateFromTransactionTimeSafe(t.getTransactionTime()); // Use a robust date parser
            if (date == null) {
                System.err.println("Skipping transaction with unparseable date for summary aggregation: " + t.getTransactionTime() + " - " + t.getOrderNumber());
                continue; // Skip transactions with invalid date
            }

            // Get month identifier (e.g., "2025-03")
            String monthIdentifier = YearMonth.from(date).format(monthFormatter);

            // Get or create the MonthlySummary object for this month
            monthlySummaries.putIfAbsent(monthIdentifier, new MonthlySummary(monthIdentifier));
            MonthlySummary currentMonthSummary = monthlySummaries.get(monthIdentifier);

            // Add transaction amount to the summary based on type (Income/Expense)
            if (t.getInOut() != null) {
                String inOut = t.getInOut().trim();
                if (inOut.equals("收入") || inOut.equals("收")) {
                    currentMonthSummary.addIncome(t.getPaymentAmount());
                } else if (inOut.equals("支出") || inOut.equals("支")) {
                    // Get the standard category for the expense
                    String rawType = t.getTransactionType();
                    // Use the helper to map to a standard category, defaulting to "其他支出" if no direct standard match
                    String standardCategory = StandardCategories.getStandardCategory(rawType);
                    // For aggregation, we might want to map any non-standard expense type to "其他支出"
                    String effectiveExpenseCategoryForSummary = StandardCategories.isStandardExpenseCategory(standardCategory) ? standardCategory : "其他支出";

                    currentMonthSummary.addExpense(t.getPaymentAmount(), effectiveExpenseCategoryForSummary);
                }
                // Ignore special types (like Transfer, Red Packet) for simple income/expense summary, or handle them separately if needed
            }
        }
        System.out.println("Generated summary for " + monthlySummaries.size() + " months.");

        return monthlySummaries; // Return the map of monthly summaries
    }

    // Helper method to parse date from transaction time string safely
    // This should be consistent across all services/DAOs that parse dates.
    // Let's use a consistent, robust parser.
    // This method is similar to parseDateTimeSafe in this class and parseDateFromTransactionTime in SummaryStatisticService.
    // Consider extracting this to a shared Util class if many places need it.
    // For now, keep a consistent copy.
    private LocalDate parseDateFromTransactionTimeSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // Clean whitespace and replace potential hyphens with slashes if the expected format is slash-separated
        String datePart = timeStr.split(" ")[0]; // Get the date part

        datePart = datePart.trim().replace('-', '/').replaceAll("\\s+", "");


        // Try parsing with multiple slash formats
        List<String> patterns = List.of(
                "yyyy/M/d", "yyyy/MM/d", "yyyy/M/dd", "yyyy/MM/dd",
                "yyyy-MM-dd" // Add dash format just in case
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(datePart, formatter);
            } catch (Exception ignored) {
                // Ignore parsing errors for this pattern
            }
        }
        System.err.println("TransactionServiceImpl: Failed to parse date part '" + datePart + "' from transaction time: " + timeStr);
        return null; // Return null if no pattern matches
    }
}
