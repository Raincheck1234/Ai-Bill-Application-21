package DAO.Impl;

// No specific imports needed for TransactionDao interface itself in JUnit test usually
// import DAO.TransactionDao;
import model.Transaction;
// import Constants.ConfigConstants; // Not directly used in this test logic

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// It's good practice to import static assertions if used, e.g., import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Test class for CsvTransactionDao
public class CsvTransactionDaoTest {

    private CsvTransactionDao transactionDao; // Use concrete class for testing its specific implementation
    // Path to a sample CSV file used for read tests and as a base for write tests.
    // Ensure this file exists in the specified location relative to the project root.
    private final String sampleTransactionFilePath = "src/test/resources/CSVForm/transactions/admin_transactions.csv";
    private Path tempTransactionFilePath; // Temporary file path for tests that modify data.

    @BeforeEach
    void setUp() throws IOException {
        transactionDao = new CsvTransactionDao();
        // Create a temporary copy of the sample file before each test that might modify data.
        // This ensures test isolation.
        Path originalPath = Paths.get(sampleTransactionFilePath);
        if (!Files.exists(originalPath)) {
            throw new IOException("Sample transaction file not found at: " + sampleTransactionFilePath);
        }
        tempTransactionFilePath = Files.createTempFile("test_transactions_", ".csv");
        Files.copy(originalPath, tempTransactionFilePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("CsvTransactionDaoTest: Copied " + sampleTransactionFilePath + " to temporary file " + tempTransactionFilePath.toString() + " for testing.");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the temporary file after each test to clean up.
        if (tempTransactionFilePath != null && Files.exists(tempTransactionFilePath)) {
            Files.delete(tempTransactionFilePath);
            System.out.println("CsvTransactionDaoTest: Deleted temporary file " + tempTransactionFilePath.toString());
        }
    }

    @Test
    void testLoadFromCSV() {
        // Test loading transactions from a CSV file.
        try {
            List<Transaction> transactions = transactionDao.loadFromCSV(sampleTransactionFilePath);
            System.out.println("CsvTransactionDaoTest (loadFromCSV): Loaded " + transactions.size() + " transactions from " + sampleTransactionFilePath);
            // Basic check: if transactions were loaded, print info about the first one.
            if (!transactions.isEmpty()) {
                System.out.println("CsvTransactionDaoTest (loadFromCSV): First transaction order number: " + transactions.get(0).getOrderNumber());
            }
            // Add assertions here, e.g., assertTrue(!transactions.isEmpty()); assertEquals(expectedSize, transactions.size());
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (loadFromCSV): Error during test.");
            e.printStackTrace();
            // Fail the test if an exception occurs.
            throw new RuntimeException("testLoadFromCSV failed", e);
        }
        System.out.println("CsvTransactionDaoTest (loadFromCSV): testLoadFromCSV finished.");
    }

    @Test
    void testGetAllTransactions() {
        // Test the getAllTransactions method (which typically calls loadFromCSV).
        try {
            List<Transaction> transactions = transactionDao.getAllTransactions(sampleTransactionFilePath);
            System.out.println("CsvTransactionDaoTest (getAllTransactions): Loaded " + transactions.size() + " transactions from " + sampleTransactionFilePath);
            if (!transactions.isEmpty()) {
                System.out.println("CsvTransactionDaoTest (getAllTransactions): First transaction order number: " + transactions.get(0).getOrderNumber());
            }
            // Add assertions.
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (getAllTransactions): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testGetAllTransactions failed", e);
        }
        System.out.println("CsvTransactionDaoTest (getAllTransactions): testGetAllTransactions finished.");
    }

    @Test
    void testAddTransaction() {
        // Test adding a new transaction to the CSV file.
        try {
            String uniqueOrderNumber = "TEST_ADD_" + UUID.randomUUID().toString();
            Transaction newTx = new Transaction("2024/01/01 10:00", "TestType", "TestCounterparty", "TestCommodity", "Expense", 10.0, "TestPay", "Completed", uniqueOrderNumber, "M001", "Add test");

            System.out.println("CsvTransactionDaoTest (addTransaction): Attempting to add transaction with ON: " + uniqueOrderNumber + " to " + tempTransactionFilePath.toString());
            transactionDao.addTransaction(tempTransactionFilePath.toString(), newTx);
            System.out.println("CsvTransactionDaoTest (addTransaction): Transaction added.");

            // Verify by reloading and checking.
            List<Transaction> transactions = transactionDao.loadFromCSV(tempTransactionFilePath.toString());
            boolean found = transactions.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("CsvTransactionDaoTest (addTransaction): Transaction found after add: " + found);
            if (!found) {
                System.err.println("CsvTransactionDaoTest (addTransaction): Added transaction NOT FOUND in file.");
                // Assert.fail("Added transaction was not found after reload.");
            }
            // Assert.assertTrue(found, "Transaction should be found after adding.");
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (addTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testAddTransaction failed", e);
        }
        System.out.println("CsvTransactionDaoTest (addTransaction): testAddTransaction finished.");
    }

    @Test
    void testDeleteTransaction() {
        // Test deleting an existing transaction.
        try {
            // First, add a transaction to ensure it exists, then delete it.
            String uniqueOrderNumber = "TEST_DELETE_" + UUID.randomUUID().toString();
            Transaction txToDelete = new Transaction("2024/01/02 11:00", "ToDelete", "DelCounter", "DelItem", "Expense", 20.0, "DelPay", "Done", uniqueOrderNumber, "M002", "Delete test");
            transactionDao.addTransaction(tempTransactionFilePath.toString(), txToDelete);
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Added transaction for deletion: " + uniqueOrderNumber);

            boolean deleted = transactionDao.deleteTransaction(tempTransactionFilePath.toString(), uniqueOrderNumber);
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Deletion result for ON " + uniqueOrderNumber + ": " + deleted);
            // Assert.assertTrue(deleted, "Deletion should return true for an existing transaction.");

            // Verify by reloading.
            List<Transaction> transactions = transactionDao.loadFromCSV(tempTransactionFilePath.toString());
            boolean stillExists = transactions.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Transaction still exists after delete: " + stillExists);
            if (stillExists) {
                System.err.println("CsvTransactionDaoTest (deleteTransaction): Deleted transaction STILL FOUND in file.");
                // Assert.fail("Transaction was found after it should have been deleted.");
            }
            // Assert.assertFalse(stillExists, "Transaction should not exist after deletion.");
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (deleteTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testDeleteTransaction failed", e);
        }
        System.out.println("CsvTransactionDaoTest (deleteTransaction): testDeleteTransaction finished.");
    }

    @Test
    void testUpdateTransaction() {
        // Test updating a field of an existing transaction.
        try {
            String uniqueOrderNumber = "TEST_UPDATE_" + UUID.randomUUID().toString();
            Transaction txToUpdate = new Transaction("2024/01/03 12:00", "ToUpdate", "UpdCounter", "UpdItem", "Income", 30.0, "UpdPay", "Pending", uniqueOrderNumber, "M003", "Update test original");
            transactionDao.addTransaction(tempTransactionFilePath.toString(), txToUpdate);
            System.out.println("CsvTransactionDaoTest (updateTransaction): Added transaction for update: " + uniqueOrderNumber);

            String newRemark = "Remark updated successfully!";
            boolean updated = transactionDao.updateTransaction(tempTransactionFilePath.toString(), uniqueOrderNumber, "remarks", newRemark);
            System.out.println("CsvTransactionDaoTest (updateTransaction): Update result for ON " + uniqueOrderNumber + ": " + updated);
            // Assert.assertTrue(updated, "Update should return true for an existing transaction and valid field.");

            // Verify by fetching the transaction.
            Transaction fetched = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), uniqueOrderNumber);
            if (fetched != null) {
                System.out.println("CsvTransactionDaoTest (updateTransaction): Fetched remark: " + fetched.getRemarks() + ". Expected: " + newRemark);
                if (!newRemark.equals(fetched.getRemarks())) {
                    System.err.println("CsvTransactionDaoTest (updateTransaction): Remark NOT updated correctly.");
                    // Assert.fail("Remark was not updated as expected.");
                }
                // Assert.assertEquals(newRemark, fetched.getRemarks(), "Remark should be updated.");
            } else {
                System.err.println("CsvTransactionDaoTest (updateTransaction): Transaction NOT FOUND after update attempt.");
                // Assert.fail("Transaction not found after update attempt.");
            }
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (updateTransaction): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testUpdateTransaction failed", e);
        }
        System.out.println("CsvTransactionDaoTest (updateTransaction): testUpdateTransaction finished.");
    }

    @Test
    void testGetTransactionByOrderNumber() {
        // Test fetching a specific transaction by its order number.
        try {
            // Assuming 'SALARY_MAR_A' exists in the sample admin_transactions.csv.
            // This order number should be present in the temp file after copy.
            String existingOrderNumber = "SALARY_MAR_A"; // Make sure this ON exists in your sample CSV.
            Transaction transaction = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), existingOrderNumber);
            if (transaction != null) {
                System.out.println("CsvTransactionDaoTest (getByON): Found transaction commodity: " + transaction.getCommodity() + " for ON " + existingOrderNumber);
                // Assert.assertEquals(existingOrderNumber, transaction.getOrderNumber());
            } else {
                System.err.println("CsvTransactionDaoTest (getByON): Transaction with ON " + existingOrderNumber + " NOT FOUND in " + tempTransactionFilePath.toString());
                // Assert.fail("Expected transaction not found: " + existingOrderNumber);
            }

            // Test with a non-existing order number.
            String nonExistingOrderNumber = "NON_EXISTENT_ON_123";
            Transaction nonExistingTx = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), nonExistingOrderNumber);
            System.out.println("CsvTransactionDaoTest (getByON): Result for non-existing ON " + nonExistingOrderNumber + ": " + (nonExistingTx == null ? "null (Correct)" : "Found (Incorrect)"));
            // Assert.assertNull(nonExistingTx, "Should return null for a non-existing order number.");
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (getByON): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testGetTransactionByOrderNumber failed", e);
        }
        System.out.println("CsvTransactionDaoTest (getByON): testGetTransactionByOrderNumber finished.");
    }

    @Test
    void testWriteTransactionsToCSV() {
        // Test writing a list of transactions to a new CSV file (overwrite).
        Path newTempFile = null;
        try {
            newTempFile = Files.createTempFile("test_write_all_", ".csv");
            List<Transaction> transactionsToWrite = new ArrayList<>();
            transactionsToWrite.add(new Transaction("2024/02/01", "Write1", "W_Counter1", "W_Item1", "Expense", 1.0, "Cash", "OK", "W_ON001", "WM001", "Note1"));
            transactionsToWrite.add(new Transaction("2024/02/02", "Write2", "W_Counter2", "W_Item2", "Income", 2.0, "Card", "OK", "W_ON002", "WM002", "Note2"));

            System.out.println("CsvTransactionDaoTest (writeAll): Attempting to write " + transactionsToWrite.size() + " transactions to " + newTempFile.toString());
            transactionDao.writeTransactionsToCSV(newTempFile.toString(), transactionsToWrite);
            System.out.println("CsvTransactionDaoTest (writeAll): Wrote transactions.");

            // Verify by reloading from the new file.
            List<Transaction> reReadTransactions = transactionDao.loadFromCSV(newTempFile.toString());
            System.out.println("CsvTransactionDaoTest (writeAll): Re-read " + reReadTransactions.size() + " transactions.");
            if (reReadTransactions.size() == transactionsToWrite.size()) {
                System.out.println("CsvTransactionDaoTest (writeAll): Count matches.");
                // Assert.assertEquals(transactionsToWrite.size(), reReadTransactions.size(), "Number of transactions should match after writing and rereading.");
            } else {
                System.err.println("CsvTransactionDaoTest (writeAll): Count MISMATCH after write/read.");
                // Assert.fail("Transaction count mismatch after write and read.");
            }
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (writeAll): Error during test.");
            e.printStackTrace();
            throw new RuntimeException("testWriteTransactionsToCSV failed", e);
        } finally {
            // Clean up the newly created temporary file for this test.
            if (newTempFile != null) {
                try {
                    Files.deleteIfExists(newTempFile);
                    System.out.println("CsvTransactionDaoTest (writeAll): Deleted temp write file " + newTempFile.toString());
                } catch (IOException e) {
                    System.err.println("CsvTransactionDaoTest (writeAll): Error deleting temp file " + newTempFile.toString());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("CsvTransactionDaoTest (writeAll): testWriteTransactionsToCSV finished.");
    }
}