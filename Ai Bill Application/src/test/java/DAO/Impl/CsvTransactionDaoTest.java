package DAO.Impl;


import DAO.Impl.CsvTransactionDao;
import DAO.TransactionDao;
import model.Transaction;
import Constants.ConfigConstants; // For base paths if needed

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CsvTransactionDaoTest {

    private TransactionDao transactionDao;
    // Use a known existing file for read tests
    private final String sampleTransactionFilePath = "src/test/resources/CSVForm/transactions/admin_transactions.csv";
    private Path tempTransactionFilePath; // For write tests

    @BeforeEach
    void setUp() throws IOException {
        transactionDao = new CsvTransactionDao();
        // Create a temporary copy of the sample file for tests that modify data
        Path originalPath = Paths.get(sampleTransactionFilePath);
        tempTransactionFilePath = Files.createTempFile("test_transactions_", ".csv");
        Files.copy(originalPath, tempTransactionFilePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("CsvTransactionDaoTest: Copied " + sampleTransactionFilePath + " to temporary file " + tempTransactionFilePath.toString() + " for testing.");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the temporary file after each test
        if (tempTransactionFilePath != null && Files.exists(tempTransactionFilePath)) {
            Files.delete(tempTransactionFilePath);
            System.out.println("CsvTransactionDaoTest: Deleted temporary file " + tempTransactionFilePath.toString());
        }
    }

    @Test
    void testLoadFromCSV() {
        try {
            List<Transaction> transactions = transactionDao.loadFromCSV(sampleTransactionFilePath);
            System.out.println("CsvTransactionDaoTest (loadFromCSV): Loaded " + transactions.size() + " transactions from " + sampleTransactionFilePath);
            if (!transactions.isEmpty()) {
                System.out.println("CsvTransactionDaoTest (loadFromCSV): First transaction: " + transactions.get(0).getOrderNumber());
            }
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (loadFromCSV): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (loadFromCSV): testLoadFromCSV finished.");
    }

    @Test
    void testGetAllTransactions() {
        try {
            List<Transaction> transactions = transactionDao.getAllTransactions(sampleTransactionFilePath);
            System.out.println("CsvTransactionDaoTest (getAllTransactions): Loaded " + transactions.size() + " transactions from " + sampleTransactionFilePath);
            if (!transactions.isEmpty()) {
                System.out.println("CsvTransactionDaoTest (getAllTransactions): First transaction: " + transactions.get(0).getOrderNumber());
            }
        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (getAllTransactions): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (getAllTransactions): testGetAllTransactions finished.");
    }

    @Test
    void testAddTransaction() {
        try {
            String uniqueOrderNumber = "TEST_ADD_" + UUID.randomUUID().toString();
            Transaction newTx = new Transaction("2024/01/01 10:00", "TestType", "TestCounterparty", "TestCommodity", "支出", 10.0, "TestPay", "Completed", uniqueOrderNumber, "M001", "Add test");

            System.out.println("CsvTransactionDaoTest (addTransaction): Attempting to add transaction with ON: " + uniqueOrderNumber + " to " + tempTransactionFilePath.toString());
            transactionDao.addTransaction(tempTransactionFilePath.toString(), newTx);
            System.out.println("CsvTransactionDaoTest (addTransaction): Transaction added.");

            List<Transaction> transactions = transactionDao.loadFromCSV(tempTransactionFilePath.toString());
            boolean found = transactions.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("CsvTransactionDaoTest (addTransaction): Transaction found after add: " + found);
            if (!found) System.err.println("CsvTransactionDaoTest (addTransaction): Added transaction NOT FOUND in file.");

        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (addTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (addTransaction): testAddTransaction finished.");
    }

    @Test
    void testDeleteTransaction() {
        try {
            // First, add a transaction to ensure it exists, then delete it
            String uniqueOrderNumber = "TEST_DELETE_" + UUID.randomUUID().toString();
            Transaction txToDelete = new Transaction("2024/01/02 11:00", "ToDelete", "DelCounter", "DelItem", "支出", 20.0, "DelPay", "Done", uniqueOrderNumber, "M002", "Delete test");
            transactionDao.addTransaction(tempTransactionFilePath.toString(), txToDelete);
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Added transaction for deletion: " + uniqueOrderNumber);

            boolean deleted = transactionDao.deleteTransaction(tempTransactionFilePath.toString(), uniqueOrderNumber);
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Deletion result for ON " + uniqueOrderNumber + ": " + deleted);

            List<Transaction> transactions = transactionDao.loadFromCSV(tempTransactionFilePath.toString());
            boolean stillExists = transactions.stream().anyMatch(t -> t.getOrderNumber().equals(uniqueOrderNumber));
            System.out.println("CsvTransactionDaoTest (deleteTransaction): Transaction still exists after delete: " + stillExists);
            if (stillExists) System.err.println("CsvTransactionDaoTest (deleteTransaction): Deleted transaction STILL FOUND in file.");


        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (deleteTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (deleteTransaction): testDeleteTransaction finished.");
    }

    @Test
    void testUpdateTransaction() {
        try {
            String uniqueOrderNumber = "TEST_UPDATE_" + UUID.randomUUID().toString();
            Transaction txToUpdate = new Transaction("2024/01/03 12:00", "ToUpdate", "UpdCounter", "UpdItem", "收入", 30.0, "UpdPay", "Pending", uniqueOrderNumber, "M003", "Update test original");
            transactionDao.addTransaction(tempTransactionFilePath.toString(), txToUpdate);
            System.out.println("CsvTransactionDaoTest (updateTransaction): Added transaction for update: " + uniqueOrderNumber);

            String newRemark = "Remark updated successfully!";
            boolean updated = transactionDao.updateTransaction(tempTransactionFilePath.toString(), uniqueOrderNumber, "remarks", newRemark);
            System.out.println("CsvTransactionDaoTest (updateTransaction): Update result for ON " + uniqueOrderNumber + ": " + updated);

            Transaction fetched = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), uniqueOrderNumber);
            if (fetched != null) {
                System.out.println("CsvTransactionDaoTest (updateTransaction): Fetched remark: " + fetched.getRemarks() + ". Expected: " + newRemark);
                if (!newRemark.equals(fetched.getRemarks())) System.err.println("CsvTransactionDaoTest (updateTransaction): Remark NOT updated correctly.");
            } else {
                System.err.println("CsvTransactionDaoTest (updateTransaction): Transaction NOT FOUND after update attempt.");
            }

        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (updateTransaction): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (updateTransaction): testUpdateTransaction finished.");
    }

    @Test
    void testGetTransactionByOrderNumber() {
        try {
            // Assuming 'SALARY_MAR_A' exists in admin_transactions.csv (copied to temp file)
            String existingOrderNumber = "SALARY_MAR_A";
            Transaction transaction = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), existingOrderNumber);
            if (transaction != null) {
                System.out.println("CsvTransactionDaoTest (getByON): Found transaction: " + transaction.getCommodity() + " for ON " + existingOrderNumber);
            } else {
                System.err.println("CsvTransactionDaoTest (getByON): Transaction with ON " + existingOrderNumber + " NOT FOUND in " + tempTransactionFilePath.toString());
            }

            String nonExistingOrderNumber = "NON_EXISTENT_ON_123";
            Transaction nonExistingTx = transactionDao.getTransactionByOrderNumber(tempTransactionFilePath.toString(), nonExistingOrderNumber);
            System.out.println("CsvTransactionDaoTest (getByON): Result for non-existing ON " + nonExistingOrderNumber + ": " + (nonExistingTx == null ? "null (Correct)" : "Found (Incorrect)"));

        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (getByON): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvTransactionDaoTest (getByON): testGetTransactionByOrderNumber finished.");
    }

    @Test
    void testWriteTransactionsToCSV() {
        Path newTempFile = null;
        try {
            newTempFile = Files.createTempFile("test_write_all_", ".csv");
            List<Transaction> transactionsToWrite = new ArrayList<>();
            transactionsToWrite.add(new Transaction("2024/02/01", "Write1", "W_Counter1", "W_Item1", "支出", 1.0, "Cash", "OK", "W_ON001", "WM001", "Note1"));
            transactionsToWrite.add(new Transaction("2024/02/02", "Write2", "W_Counter2", "W_Item2", "收入", 2.0, "Card", "OK", "W_ON002", "WM002", "Note2"));

            System.out.println("CsvTransactionDaoTest (writeAll): Attempting to write " + transactionsToWrite.size() + " transactions to " + newTempFile.toString());
            transactionDao.writeTransactionsToCSV(newTempFile.toString(), transactionsToWrite);
            System.out.println("CsvTransactionDaoTest (writeAll): Wrote transactions.");

            List<Transaction> reReadTransactions = transactionDao.loadFromCSV(newTempFile.toString());
            System.out.println("CsvTransactionDaoTest (writeAll): Re-read " + reReadTransactions.size() + " transactions.");
            if (reReadTransactions.size() == transactionsToWrite.size()) {
                System.out.println("CsvTransactionDaoTest (writeAll): Count matches.");
            } else {
                System.err.println("CsvTransactionDaoTest (writeAll): Count MISMATCH after write/read.");
            }

        } catch (Exception e) {
            System.err.println("CsvTransactionDaoTest (writeAll): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (newTempFile != null) {
                try {
                    Files.deleteIfExists(newTempFile);
                    System.out.println("CsvTransactionDaoTest (writeAll): Deleted temp write file " + newTempFile.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("CsvTransactionDaoTest (writeAll): testWriteTransactionsToCSV finished.");
    }
}