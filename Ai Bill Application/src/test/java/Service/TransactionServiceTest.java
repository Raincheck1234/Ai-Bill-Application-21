package Service;

import DAO.Impl.CsvTransactionDao; // This import implies you might be using it directly, which is okay for test setup
import Service.Impl.TransactionServiceImpl;
import model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files; // Added for file operations if needed for setup
import java.nio.file.Path;   // Added for file operations
import java.nio.file.Paths; // Added for file operations
import java.nio.file.StandardCopyOption; // Added for file operations
import java.util.List;

// Test class for TransactionService
class TransactionServiceTest {
    private TransactionService transactionService;
    // Define a temporary file path for tests to operate on, to avoid affecting real data
    private static final String TEST_TRANSACTION_FILE_PATH = "target/test-transactions.csv"; // Example path, adjust as needed
    private static final String SAMPLE_TRANSACTION_FILE_PATH = "src/test/resources/CSVForm/transactions/user1_transactions.csv"; // A sample file to copy from

    @BeforeEach
    void setUp() throws IOException {
        // Initialize DAO and Service
        // For TransactionServiceImpl, it now expects a file path.
        // For isolated testing, it's best to use a temporary or test-specific file.
        // Copy a sample file to the test path before each test.
        Path sourcePath = Paths.get(SAMPLE_TRANSACTION_FILE_PATH);
        Path destinationPath = Paths.get(TEST_TRANSACTION_FILE_PATH);
        // Ensure parent directory for destinationPath exists
        if (destinationPath.getParent() != null) {
            Files.createDirectories(destinationPath.getParent());
        }
        if (Files.exists(sourcePath)) {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // If no sample, create an empty file with headers for some tests to work (like add)
            // This part depends on how CsvTransactionDao handles empty files or if it requires headers.
            // For simplicity, this example assumes a sample file is copied.
            // If not, you might need to create an empty file or a file with just headers.
            System.err.println("Warning: Sample transaction file not found at " + SAMPLE_TRANSACTION_FILE_PATH + ". Tests might behave unexpectedly.");
            // Create an empty file if no sample (some tests might fail if they expect data)
            Files.deleteIfExists(destinationPath); // Delete if exists to start fresh
            Files.createFile(destinationPath); // Create empty file
            // Optionally write headers here if your DAO expects them for an "empty" file
        }

        transactionService = new TransactionServiceImpl(TEST_TRANSACTION_FILE_PATH);
    }

    @Test
    void testAddTransaction() throws IOException {
        // Prepare test data
        Transaction transaction = new Transaction(
                "2023-08-20 15:30:00", "Transfer", "Li Si", "Virtual Product", "Expense", 500.0,
                "Bank Card", "Completed", "T123456789", "M987654321", "Test"
        );

        // Perform add operation
        transactionService.addTransaction(transaction);

        // Verify if added successfully
        // Searching with an empty criteria should return all transactions
        List<Transaction> transactions = transactionService.searchTransaction(new Transaction());
        assertFalse(transactions.isEmpty(), "Transaction list should not be empty after adding.");
        // Check if the added transaction is present
        assertTrue(transactions.stream().anyMatch(t -> "T123456789".equals(t.getOrderNumber())),
                "Added transaction with OrderNumber T123456789 should be found.");
    }

    @Test
    void testChangeTransaction() throws Exception {
        // Prepare test data
        String orderNumber = "T_CHANGE_001";
        Transaction originalTransaction = new Transaction(
                "2023-08-20 15:30:00", "Transfer", "Li Si", "Virtual Product", "Expense", 500.0,
                "Bank Card", "Completed", orderNumber, "M987654321", "Original Test"
        );
        transactionService.addTransaction(originalTransaction); // Add it first

        // Prepare update data
        Transaction updatedTransactionInfo = new Transaction(
                null, "Recharge", null, null, null, 0.0,
                "WeChat Pay", null, orderNumber, null, "Updated Remark"
        ); // Only fields to be updated are set, plus the OrderNumber key

        // Perform update operation
        transactionService.changeTransaction(updatedTransactionInfo);

        // Verify if updated successfully
        Transaction searchCriteria = new Transaction();
        searchCriteria.setOrderNumber(orderNumber); // Search specifically for the updated transaction
        List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);

        assertFalse(transactions.isEmpty(), "Updated transaction should be found.");
        assertEquals(1, transactions.size(), "Should find exactly one transaction with the order number.");
        Transaction fetchedTransaction = transactions.get(0);
        assertEquals("Recharge", fetchedTransaction.getTransactionType(), "Transaction type should be updated.");
        assertEquals("WeChat Pay", fetchedTransaction.getPaymentMethod(), "Payment method should be updated.");
        assertEquals("Updated Remark", fetchedTransaction.getRemarks(), "Remarks should be updated.");
        // Verify that unchanged fields remain the same
        assertEquals("Li Si", fetchedTransaction.getCounterparty(), "Counterparty should remain unchanged.");
        assertEquals(500.0, fetchedTransaction.getPaymentAmount(), 0.01, "Amount should remain unchanged as per update logic.");
    }

    @Test
    void testDeleteTransaction() throws Exception {
        // Prepare test data
        String orderNumberToDelete = "T_DELETE_002";
        Transaction transaction = new Transaction(
                "2023-08-21 10:00:00", "Sale", "Wang Wu", "Physical Product", "Income", 300.0,
                "Alipay", "Completed", orderNumberToDelete, "M_DEL_002", "To Delete"
        );
        transactionService.addTransaction(transaction); // Add it first

        // Perform delete operation
        boolean result = transactionService.deleteTransaction(orderNumberToDelete);

        // Verify if deleted successfully
        assertTrue(result, "Deletion should be successful for an existing transaction.");
        Transaction searchCriteria = new Transaction();
        searchCriteria.setOrderNumber(orderNumberToDelete);
        List<Transaction> transactions = transactionService.searchTransaction(searchCriteria);
        assertTrue(transactions.isEmpty(), "Transaction should no longer be found after deletion.");
    }

    @Test
    void testSearchTransaction() throws IOException {
        // Setup: Add a specific transaction to ensure search criteria can find something
        Transaction txToFind = new Transaction(
                "2023-08-22 11:00:00", "Payment", "Alipay", "Service Fee", "Expense", 25.0,
                "Balance", "Done", "T_SEARCH_003", "M_SEARCH_003", "Alipay search test"
        );
        transactionService.addTransaction(txToFind);


        // Set search criteria
        Transaction searchCriteria = new Transaction();
        searchCriteria.setCounterparty("Alipay"); // Search for transactions with counterparty "Alipay"

        // Perform search operation
        List<Transaction> result = transactionService.searchTransaction(searchCriteria);

        // Verify search result
        assertFalse(result.isEmpty(), "Search results should not be empty if matching data exists/was added.");
        result.forEach(res -> {
            System.out.println("Found Commodity: " + res.getCommodity() + " for Counterparty: " + res.getCounterparty());
            assertEquals("Alipay", res.getCounterparty(), "All results should have 'Alipay' as counterparty.");
        });
    }
}