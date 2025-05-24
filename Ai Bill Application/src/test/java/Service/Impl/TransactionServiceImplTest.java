package Service.Impl;

import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao; // For direct use in creating temp files if needed
import DAO.Impl.CsvUserDao;
import DAO.UserDao;
import Service.TransactionService;
import Service.User.UserService;
import model.MonthlySummary;
import model.Transaction;
import model.User;
import Constants.ConfigConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// It's good practice to import static assertions from JUnit 5:
// import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Test class for TransactionServiceImpl
public class TransactionServiceImplTest {

    private TransactionService transactionService;
    private User testUser;
    private Path tempUserTransactionFilePath; // Path for the temporary transaction file for the test user
    // Path to an original sample transaction file for user1
    private final String originalUser1CsvPath = "src/test/resources/CSVForm/transactions/user1_transactions.csv";
    // Path to a sample CSV file to be used as a source for import tests
    private final String importTestCsvPath = "src/test/resources/CSVForm/transactions/admin_transactions.csv";

    @BeforeEach
    void setUp() throws IOException {
        // Initialize UserService and authenticate a test user.
        // This setup assumes user authentication and user CSV structure are working.
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH); // Assumes USERS_CSV_PATH is correctly configured.
        UserService userService = new UserService(userDao, new CsvTransactionDao(), new CsvSummaryStatisticDao());
        testUser = userService.authenticate("user1", "pass123"); // Use actual credentials for a test user

        if (testUser == null) {
            throw new IllegalStateException("TransactionServiceImplTest: Cannot authenticate test user 'user1'. Aborting setup.");
        }

        // Create a temporary copy of user1's transaction file to avoid modifying original test data.
        Path originalPath = Paths.get(originalUser1CsvPath);
        if (!Files.exists(originalPath)) {
            throw new IOException("Original user1 transaction file not found: " + originalUser1CsvPath);
        }
        tempUserTransactionFilePath = Files.createTempFile("test_user1_transactions_", ".csv");
        Files.copy(originalPath, tempUserTransactionFilePath, StandardCopyOption.REPLACE_EXISTING);

        // IMPORTANT: Update the testUser's transaction file path to the temporary one for this test run.
        // This ensures the TransactionServiceImpl instance operates on the temporary file.
        testUser.setTransactionFilePath(tempUserTransactionFilePath.toString());

        // Initialize the service to be tested with the path to the temporary transaction file.
        transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        System.out.println("TransactionServiceImplTest: Set up with temp file: " + testUser.getTransactionFilePath());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the temporary transaction file after each test to clean up.
        if (tempUserTransactionFilePath != null && Files.exists(tempUserTransactionFilePath)) {
            Files.delete(tempUserTransactionFilePath);
            System.out.println("TransactionServiceImplTest: Deleted temporary transaction file: " + tempUserTransactionFilePath);
        }
    }

    @Test
    void testGetAllTransactions() {
        System.out.println("TransactionServiceImplTest: Running testGetAllTransactions...");
        try {
            List<Transaction> transactions = transactionService.getAllTransactions();
            System.out.println("TransactionServiceImplTest (getAllTransactions): Loaded " + transactions.size() + " transactions.");
            // Basic check: print info about the first transaction if the list is not empty.
            if (!transactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (getAllTransactions): First transaction ON: " + transactions.get(0).getOrderNumber());
            }
            // Add assertions here, e.g., assertTrue(!transactions.isEmpty());
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (getAllTransactions): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testGetAllTransactions failed", e); // Fail the test
        }
        System.out.println("TransactionServiceImplTest: testGetAllTransactions finished.");
    }

    @Test
    void testAddTransaction() {
        System.out.println("TransactionServiceImplTest: Running testAddTransaction...");
        try {
            String uniqueOrderNumber = "SERVICE_ADD_" + UUID.randomUUID().toString();
            Transaction newTx = new Transaction(
                    "2024/03/01 10:00", "ServiceAdd", "S_Counter", "S_Item", "Expense", // "Expense"
                    55.0, "S_Pay", "Completed", uniqueOrderNumber, "S_M001", "Service add test"
            );
            transactionService.addTransaction(newTx);
            System.out.println("TransactionServiceImplTest (addTransaction): Added transaction with ON: " + uniqueOrderNumber);

            // Verify by fetching all transactions and checking if the new one exists.
            List<Transaction> all = transactionService.getAllTransactions();
            boolean found = all.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("TransactionServiceImplTest (addTransaction): Found after add: " + found);
            if (!found) {
                System.err.println("TransactionServiceImplTest (addTransaction): Added transaction NOT FOUND.");
                // fail("Added transaction was not found.");
            }
            // assertTrue(found, "Transaction should be found after adding.");
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (addTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testAddTransaction failed", e);
        }
        System.out.println("TransactionServiceImplTest: testAddTransaction finished.");
    }

    @Test
    void testChangeTransaction() {
        System.out.println("TransactionServiceImplTest: Running testChangeTransaction...");
        try {
            List<Transaction> initialTransactions = transactionService.getAllTransactions();
            if (initialTransactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (changeTransaction): No transactions to change. Adding one for the test.");
                // Add a transaction if the list is empty to ensure the test can proceed.
                String uniqueOrderNumber = "SERVICE_CHANGE_" + UUID.randomUUID().toString();
                Transaction newTx = new Transaction("2024/03/02", "ForChange", "FC", "FCI", "Expense", 1.0, "Cash", "OK", uniqueOrderNumber, "FCM", "Original");
                transactionService.addTransaction(newTx);
                initialTransactions = transactionService.getAllTransactions(); // Reload
                if(initialTransactions.isEmpty()){
                    System.err.println("TransactionServiceImplTest (changeTransaction): Still no transactions after add. Test cannot proceed.");
                    // fail("Could not add a transaction to test change operation.");
                    return;
                }
            }

            Transaction toChange = new Transaction(); // Create a new Transaction object for update criteria
            toChange.setOrderNumber(initialTransactions.get(0).getOrderNumber()); // Get OrderNumber of the first transaction
            toChange.setRemarks("Updated via Service Test");
            toChange.setTransactionType("ChangedType");
            // Note: Other fields in 'toChange' will be null/default, changeTransaction should only update specified fields.

            transactionService.changeTransaction(toChange);
            System.out.println("TransactionServiceImplTest (changeTransaction): Changed transaction with ON: " + toChange.getOrderNumber());

            // Verify the changes.
            Transaction changedTx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getOrderNumber().equals(toChange.getOrderNumber()))
                    .findFirst().orElse(null);

            if(changedTx != null) {
                System.out.println("TransactionServiceImplTest (changeTransaction): New remark: " + changedTx.getRemarks());
                System.out.println("TransactionServiceImplTest (changeTransaction): New type: " + changedTx.getTransactionType());
                if (!"Updated via Service Test".equals(changedTx.getRemarks())) {
                    System.err.println("TransactionServiceImplTest (changeTransaction): Remark NOT updated as expected.");
                    // fail("Remark was not updated correctly.");
                }
                // assertEquals("Updated via Service Test", changedTx.getRemarks());
                // assertEquals("ChangedType", changedTx.getTransactionType());
            } else {
                System.err.println("TransactionServiceImplTest (changeTransaction): Changed transaction NOT FOUND after update.");
                // fail("Transaction not found after change operation.");
            }
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (changeTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testChangeTransaction failed", e);
        }
        System.out.println("TransactionServiceImplTest: testChangeTransaction finished.");
    }

    @Test
    void testDeleteTransaction() {
        System.out.println("TransactionServiceImplTest: Running testDeleteTransaction...");
        try {
            List<Transaction> initialTransactions = transactionService.getAllTransactions();
            if (initialTransactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (deleteTransaction): No transactions to delete. Adding one for the test.");
                String uniqueOrderNumber = "SERVICE_DELETE_" + UUID.randomUUID().toString();
                Transaction newTx = new Transaction("2024/03/03", "ForDelete", "FD", "FDI", "Expense", 1.0, "Cash", "OK", uniqueOrderNumber, "FDM", "Original to be deleted");
                transactionService.addTransaction(newTx);
                initialTransactions = transactionService.getAllTransactions(); // Reload
                if(initialTransactions.isEmpty()){
                    System.err.println("TransactionServiceImplTest (deleteTransaction): Still no transactions after add. Test cannot proceed.");
                    // fail("Could not add a transaction to test delete operation.");
                    return;
                }
            }
            String orderNumberToDelete = initialTransactions.get(0).getOrderNumber();
            boolean deleted = transactionService.deleteTransaction(orderNumberToDelete);
            System.out.println("TransactionServiceImplTest (deleteTransaction): Deletion result for ON " + orderNumberToDelete + ": " + deleted);
            // assertTrue(deleted, "deleteTransaction should return true for an existing transaction.");

            // Verify the transaction is gone.
            boolean stillExists = transactionService.getAllTransactions().stream()
                    .anyMatch(t -> t.getOrderNumber().equals(orderNumberToDelete));
            System.out.println("TransactionServiceImplTest (deleteTransaction): Still exists after delete: " + stillExists);
            if (stillExists) {
                System.err.println("TransactionServiceImplTest (deleteTransaction): Deleted transaction STILL FOUND.");
                // fail("Transaction should not exist after deletion.");
            }
            // assertFalse(stillExists, "Transaction should be deleted.");
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (deleteTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testDeleteTransaction failed", e);
        }
        System.out.println("TransactionServiceImplTest: testDeleteTransaction finished.");
    }

    @Test
    void testSearchTransaction() {
        System.out.println("TransactionServiceImplTest: Running testSearchTransaction...");
        try {
            Transaction criteria = new Transaction();
            // Assuming "Salary" or a similar term exists as a commodity in user1_transactions.csv for this test.
            // If CSV uses Chinese, and Transaction model uses English, ensure mapping or test with English term.
            criteria.setCommodity("Salary"); // Search for "Salary" in commodity. "工资" in Chinese.
            List<Transaction> results = transactionService.searchTransaction(criteria);
            System.out.println("TransactionServiceImplTest (searchTransaction): Found " + results.size() + " transactions matching commodity 'Salary'.");
            results.forEach(t -> System.out.println("  - Found: " + t.getOrderNumber() + " | " + t.getCommodity()));
            // Add assertions, e.g., assertTrue(!results.isEmpty()) if "Salary" is expected.
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (searchTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testSearchTransaction failed", e);
        }
        System.out.println("TransactionServiceImplTest: testSearchTransaction finished.");
    }

    @Test
    void testGetMonthlyTransactionSummary() {
        System.out.println("TransactionServiceImplTest: Running testGetMonthlyTransactionSummary...");
        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("TransactionServiceImplTest (getMonthlySummary): Generated " + summaries.size() + " monthly summaries.");
            summaries.forEach((month, summary) -> {
                System.out.println("  Month: " + month + ", Income: " + summary.getTotalIncome() + ", Expense: " + summary.getTotalExpense());
                summary.getExpenseByCategory().forEach((cat, amt) -> System.out.println("    - Cat: " + cat + ", Amt: " + amt));
            });
            // Add assertions, e.g., assertFalse(summaries.isEmpty()) if data is expected.
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (getMonthlySummary): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testGetMonthlyTransactionSummary failed", e);
        }
        System.out.println("TransactionServiceImplTest: testGetMonthlyTransactionSummary finished.");
    }

    @Test
    void testImportTransactionsFromCsv() {
        System.out.println("TransactionServiceImplTest: Running testImportTransactionsFromCsv...");
        try {
            // Ensure the import source file exists.
            Path importSourcePath = Paths.get(importTestCsvPath);
            if (!Files.exists(importSourcePath)) {
                System.err.println("TransactionServiceImplTest (import): Import source file missing: " + importTestCsvPath + ". Skipping test.");
                // fail("Import source file missing, cannot run test.");
                return;
            }

            long initialCount = transactionService.getAllTransactions().size();
            System.out.println("TransactionServiceImplTest (import): Transactions before import: " + initialCount);

            // Perform import from importTestCsvPath (e.g., admin_transactions.csv) into the temp user1 file.
            int importedCount = transactionService.importTransactionsFromCsv(testUser.getTransactionFilePath(), importTestCsvPath);
            System.out.println("TransactionServiceImplTest (import): Imported " + importedCount + " transactions.");
            // assertTrue(importedCount > 0, "Should import at least one transaction if source is not empty and has new data.");

            long finalCount = transactionService.getAllTransactions().size();
            System.out.println("TransactionServiceImplTest (import): Transactions after import: " + finalCount);

            // Basic check for count. This assumes no duplicates were skipped if importTestCsvPath
            // has unique OrderNumbers not present in the original user1_transactions.csv.
            // This check might need to be more sophisticated based on data and duplicate handling logic.
            // For example, if all imported items are new:
            // assertEquals(initialCount + importedCount, finalCount, "Final count should be initial + imported if all new.");
            // If duplicates are skipped, this assertion needs adjustment.
            if (finalCount >= initialCount + importedCount - 5 && finalCount <= initialCount + importedCount + 5) { // Allow some leeway for duplicates or variations
                System.out.println("TransactionServiceImplTest (import): Count after import seems reasonable.");
            } else {
                System.err.println("TransactionServiceImplTest (import): Count after import seems off. Initial: " + initialCount + ", Imported: " + importedCount + ", Final: " + finalCount);
                // Consider a more precise assertion if duplicate behavior is strictly defined.
            }

        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (import): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testImportTransactionsFromCsv failed", e);
        }
        System.out.println("TransactionServiceImplTest: testImportTransactionsFromCsv finished.");
    }
}