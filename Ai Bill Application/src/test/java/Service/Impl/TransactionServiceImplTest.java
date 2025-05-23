package Service.Impl;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransactionServiceImplTest {

    private TransactionService transactionService;
    private User testUser;
    private Path tempUserTransactionFilePath;
    private final String originalUser1CsvPath = "src/test/resources/CSVForm/transactions/user1_transactions.csv";
    private final String importTestCsvPath = "src/test/resources/CSVForm/transactions/admin_transactions.csv"; // Use admin as an import source

    @BeforeEach
    void setUp() throws IOException {
        UserDao userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        UserService userService = new UserService(userDao);
        testUser = userService.authenticate("user1", "pass123");

        if (testUser == null) {
            throw new IllegalStateException("TransactionServiceImplTest: Cannot authenticate test user 'user1'. Aborting setup.");
        }

        // Create a temporary copy of user1's transaction file
        Path originalPath = Paths.get(originalUser1CsvPath);
        tempUserTransactionFilePath = Files.createTempFile("test_user1_transactions_", ".csv");
        Files.copy(originalPath, tempUserTransactionFilePath, StandardCopyOption.REPLACE_EXISTING);

        // IMPORTANT: Update the testUser's transaction file path to the temporary one for this test run
        testUser.setTransactionFilePath(tempUserTransactionFilePath.toString());

        transactionService = new TransactionServiceImpl(testUser.getTransactionFilePath());
        System.out.println("TransactionServiceImplTest: Set up with temp file: " + testUser.getTransactionFilePath());
    }

    @AfterEach
    void tearDown() throws IOException {
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
            if (!transactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (getAllTransactions): First transaction ON: " + transactions.get(0).getOrderNumber());
            }
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (getAllTransactions): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testGetAllTransactions finished.");
    }

    @Test
    void testAddTransaction() {
        System.out.println("TransactionServiceImplTest: Running testAddTransaction...");
        try {
            String uniqueOrderNumber = "SERVICE_ADD_" + UUID.randomUUID().toString();
            Transaction newTx = new Transaction(
                    "2024/03/01 10:00", "ServiceAdd", "S_Counter", "S_Item", "支出",
                    55.0, "S_Pay", "Completed", uniqueOrderNumber, "S_M001", "Service add test"
            );
            transactionService.addTransaction(newTx);
            System.out.println("TransactionServiceImplTest (addTransaction): Added transaction with ON: " + uniqueOrderNumber);

            List<Transaction> all = transactionService.getAllTransactions();
            boolean found = all.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("TransactionServiceImplTest (addTransaction): Found after add: " + found);
            if (!found) System.err.println("TransactionServiceImplTest (addTransaction): Added transaction NOT FOUND.");

        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (addTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testAddTransaction finished.");
    }

    @Test
    void testChangeTransaction() {
        System.out.println("TransactionServiceImplTest: Running testChangeTransaction...");
        try {
            List<Transaction> initialTransactions = transactionService.getAllTransactions();
            if (initialTransactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (changeTransaction): No transactions to change. Skipping actual change.");
                // Add one to change
                String uniqueOrderNumber = "SERVICE_CHANGE_" + UUID.randomUUID().toString();
                Transaction newTx = new Transaction("2024/03/02", "ForChange", "FC", "FCI", "支出", 1.0, "Cash", "OK", uniqueOrderNumber, "FCM", "Original");
                transactionService.addTransaction(newTx);
                initialTransactions = transactionService.getAllTransactions();
                if(initialTransactions.isEmpty()){
                    System.err.println("TransactionServiceImplTest (changeTransaction): Still no transactions after add. Test cannot proceed.");
                    return;
                }
            }

            Transaction toChange = new Transaction();
            toChange.setOrderNumber(initialTransactions.get(0).getOrderNumber()); // Get ON of first transaction
            toChange.setRemarks("Updated via Service Test");
            toChange.setTransactionType("ChangedType");

            transactionService.changeTransaction(toChange);
            System.out.println("TransactionServiceImplTest (changeTransaction): Changed transaction with ON: " + toChange.getOrderNumber());

            Transaction changedTx = transactionService.getAllTransactions().stream()
                    .filter(t -> t.getOrderNumber().equals(toChange.getOrderNumber()))
                    .findFirst().orElse(null);

            if(changedTx != null) {
                System.out.println("TransactionServiceImplTest (changeTransaction): New remark: " + changedTx.getRemarks());
                System.out.println("TransactionServiceImplTest (changeTransaction): New type: " + changedTx.getTransactionType());
                if (!"Updated via Service Test".equals(changedTx.getRemarks())) System.err.println("TransactionServiceImplTest (changeTransaction): Remark NOT updated.");
            } else {
                System.err.println("TransactionServiceImplTest (changeTransaction): Changed transaction NOT FOUND.");
            }

        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (changeTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testChangeTransaction finished.");
    }

    @Test
    void testDeleteTransaction() {
        System.out.println("TransactionServiceImplTest: Running testDeleteTransaction...");
        try {
            List<Transaction> initialTransactions = transactionService.getAllTransactions();
            if (initialTransactions.isEmpty()) {
                System.out.println("TransactionServiceImplTest (deleteTransaction): No transactions to delete. Skipping actual delete.");
                // Add one to delete
                String uniqueOrderNumber = "SERVICE_DELETE_" + UUID.randomUUID().toString();
                Transaction newTx = new Transaction("2024/03/03", "ForDelete", "FD", "FDI", "支出", 1.0, "Cash", "OK", uniqueOrderNumber, "FDM", "Original");
                transactionService.addTransaction(newTx);
                initialTransactions = transactionService.getAllTransactions();
                if(initialTransactions.isEmpty()){
                    System.err.println("TransactionServiceImplTest (deleteTransaction): Still no transactions after add. Test cannot proceed.");
                    return;
                }
            }
            String orderNumberToDelete = initialTransactions.get(0).getOrderNumber();
            boolean deleted = transactionService.deleteTransaction(orderNumberToDelete);
            System.out.println("TransactionServiceImplTest (deleteTransaction): Deletion result for ON " + orderNumberToDelete + ": " + deleted);

            boolean stillExists = transactionService.getAllTransactions().stream()
                    .anyMatch(t -> t.getOrderNumber().equals(orderNumberToDelete));
            System.out.println("TransactionServiceImplTest (deleteTransaction): Still exists after delete: " + stillExists);
            if (stillExists) System.err.println("TransactionServiceImplTest (deleteTransaction): Deleted transaction STILL FOUND.");


        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (deleteTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testDeleteTransaction finished.");
    }

    @Test
    void testSearchTransaction() {
        System.out.println("TransactionServiceImplTest: Running testSearchTransaction...");
        try {
            Transaction criteria = new Transaction();
            criteria.setCommodity("工资"); // Search for "工资" in commodity (assuming it exists in user1's data)
            List<Transaction> results = transactionService.searchTransaction(criteria);
            System.out.println("TransactionServiceImplTest (searchTransaction): Found " + results.size() + " transactions matching commodity '工资'.");
            results.forEach(t -> System.out.println("  - Found: " + t.getOrderNumber() + " | " + t.getCommodity()));
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (searchTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
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
        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (getMonthlySummary): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testGetMonthlyTransactionSummary finished.");
    }

    @Test
    void testImportTransactionsFromCsv() {
        System.out.println("TransactionServiceImplTest: Running testImportTransactionsFromCsv...");
        try {
            // Ensure the import source file exists
            Path importSourcePath = Paths.get(importTestCsvPath);
            if (!Files.exists(importSourcePath)) {
                System.err.println("TransactionServiceImplTest (import): Import source file missing: " + importTestCsvPath + ". Skipping test.");
                return;
            }

            long initialCount = transactionService.getAllTransactions().size();
            System.out.println("TransactionServiceImplTest (import): Transactions before import: " + initialCount);

            // Perform import from admin_transactions.csv into the temp user1 file
            int importedCount = transactionService.importTransactionsFromCsv(testUser.getTransactionFilePath(), importTestCsvPath);
            System.out.println("TransactionServiceImplTest (import): Imported " + importedCount + " transactions.");

            long finalCount = transactionService.getAllTransactions().size();
            System.out.println("TransactionServiceImplTest (import): Transactions after import: " + finalCount);

            // Basic check, assumes no duplicates were skipped if importTestCsvPath has unique ONs not in original user1
            // This check might be more complex depending on data and duplicate handling.
            if (finalCount >= initialCount + importedCount - 5 && finalCount <= initialCount + importedCount + 5) { // Allow some leeway for duplicates
                System.out.println("TransactionServiceImplTest (import): Count after import seems reasonable.");
            } else {
                System.err.println("TransactionServiceImplTest (import): Count after import seems off. Initial: " + initialCount + ", Imported: " + importedCount + ", Final: " + finalCount);
            }

        } catch (Exception e) {
            System.err.println("TransactionServiceImplTest (import): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("TransactionServiceImplTest: testImportTransactionsFromCsv finished.");
    }
}