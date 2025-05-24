package DAOTest;

import DAO.Impl.CsvTransactionDao;
import model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static Constants.ConfigConstants.CSV_PATH;
import static org.junit.Assert.*; // Using JUnit 4 Assert for assertEquals, assertNotNull etc.

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List; // Keep this import as it's used.
import java.util.ArrayList; // Added this import as List.of() is used in createTestCsvFile and its usage might imply ArrayList for modifications later.


class CsvTransactionDaoTest {
    private static final String TEST_ADMIN_CSV_PATH = "Ai Bill Application/src/test/resources/CSVForm/transactions/admin_transactions.csv"; // Path to admin CSV for testing

    // Test file path (adjust according to actual structure)
    private static final String TEST_CSV_PATH = CSV_PATH; // General test CSV path from constants
    private static CsvTransactionDao dao;

    @Test
    void testLoadFromCSV_ValidFile_ReturnsTransactions() throws Exception {
        // Given
        CsvTransactionDao dao = new CsvTransactionDao();

        // When
        List<Transaction> transactions = dao.loadFromCSV(CSV_PATH);

        // Verify fields of the first record (or all records)
        for (int i = 0; i < transactions.size(); i++) {
            System.out.println(transactions.get(i).getRemarks());
            System.out.println(transactions.get(i).getCommodity());
        }
    }

    @Test
    void testAddTransaction() throws IOException {
        dao = new CsvTransactionDao();
        Transaction newTx = new Transaction(
                "2025-03-09 10:00",
                "Transfer",           // "Transfer"
                "Xiao Ming",          // "Xiao Ming"
                "Books",              // "Books"
                "Out",                // "Out" (Assuming "支" means "Out" or "Expense")
                99.99,
                "WeChat",             // "WeChat"
                "Completed",          // "Completed"
                "TX123456",
                "M789012",
                "");

        dao.addTransaction("src/test/resources/001.csv", newTx); // Path to a specific test file for adding

        List<Transaction> transactions = dao.loadFromCSV(TEST_CSV_PATH); // Load from the general test path to verify
    }

    @BeforeEach
        // This runs before each test method
    void setUp() {
        // Initialize DAO before each test
        dao = new CsvTransactionDao();
        // Ensure the test file exists - maybe create it programmatically here for reliable testing
        // or rely on it being present in src/test/resources and copied to classpath
    }

    @Test
    void testLoadAdminCSV() throws IOException {
        System.out.println("Attempting to load test CSV: " + TEST_ADMIN_CSV_PATH);
        Path csvPath = Paths.get(TEST_ADMIN_CSV_PATH);
        assertTrue("Test CSV file should exist at " + TEST_ADMIN_CSV_PATH, Files.exists(csvPath));
        assertTrue("Test CSV file should not be empty.", Files.size(csvPath) > 0);

        // When loading the specific admin CSV
        List<Transaction> transactions = dao.loadFromCSV(TEST_ADMIN_CSV_PATH);

        // Then assert that loading was successful and data is present
        assertNotNull(transactions.toString(), "Loaded transactions list should not be null");
        assertFalse("Loaded transactions list should not be empty", transactions.isEmpty());
        // Assuming the DAO returns 5 data rows. Adjust if header affects count.
        assertEquals("Should load 5 transaction records", 5, transactions.size());

        // Optional: Verify content of a specific row
        Transaction firstTx = transactions.get(0);
        // Assuming the Transaction object field names are English, and CSV values map to them.
        // If CSV values are Chinese and DAO maps them:
        assertEquals("CompanyA", firstTx.getCounterparty()); // Example: "公司A" maps to CompanyA
        assertEquals("March Salary", firstTx.getCommodity());   // Example: "三月工资" maps to March Salary
        assertEquals(10000.00, firstTx.getPaymentAmount(), 0.01); // Use delta for double comparison
    }

    // Add other tests like testAddTransaction, testDeleteTransaction, testChangeInformation etc.
    // Ensure these tests also use the correct file paths and verify file content changes.
    // For modification/deletion tests, you might need to create a temporary CSV file
    // or use a file specifically for testing that can be modified without affecting other tests.

    // Example of a helper method to create a test CSV file programmatically
    // This is more reliable than relying on manual copying/pasting for tests.
    private void createTestCsvFile(String filePath, List<Transaction> transactions) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        // Delete old file if it exists
        if (Files.exists(path)) {
            Files.delete(path);
        }

        // Assuming English headers for consistency with most CSV libraries' defaults
        // If your DAO strictly expects Chinese headers, use the Chinese header array.
        String[] headers = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks"};
        // Chinese version for reference:
        // String[] headers = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8); // Use UTF-8 for potential Chinese characters in data
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers).withTrim())) {

            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionTime(),
                        t.getTransactionType(),
                        t.getCounterparty(),
                        t.getCommodity(),
                        t.getInOut(), // Expecting English like "Income", "Expense"
                        String.format("¥%.2f", t.getPaymentAmount()), // Or your DAO might handle currency symbol differently
                        t.getPaymentMethod(),
                        t.getCurrentStatus(),
                        t.getOrderNumber(),
                        t.getMerchantNumber(),
                        t.getRemarks()
                );
            }
        }
    }

    @Test
    void testAddTransactionToFile() throws IOException {
        // Create a temporary test file path or use a dedicated test file name
        String tempFilePath = "Ai Bill Application/src/main/resources/CSVForm/transactions/test_add.csv"; // Path might need adjustment for test resources
        // Create an empty or initial test file
        createTestCsvFile(tempFilePath, new ArrayList<>()); // Start with an empty file

        CsvTransactionDao testDao = new CsvTransactionDao(); // Or reuse the instance from BeforeEach if path is managed

        Transaction newTx = new Transaction(
                "2025/04/11 08:00:00",
                "TestType",        // "Test Type"
                "TestCounterparty",// "Test Counterparty"
                "TestCommodity",   // "Test Commodity"
                "Income",          // "Income"
                123.45,
                "TestMethod",      // "Test Method"
                "TestStatus",      // "Test Status"
                "TEST001",
                "MERCHANT001",
                "TestRemark"       // "Test Remark"
        );

        // Add the transaction
        testDao.addTransaction(tempFilePath, newTx);

        // Load the file back and verify
        List<Transaction> transactions = testDao.loadFromCSV(tempFilePath);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        Transaction addedTx = transactions.get(0);
        assertEquals("TEST001", addedTx.getOrderNumber());
        assertEquals(123.45, addedTx.getPaymentAmount(), 0.01);

        // Clean up the test file (optional but good practice)
        Files.deleteIfExists(Paths.get(tempFilePath));
    }

    @Test
    void testDeleteTransaction() throws IOException {
        dao = new CsvTransactionDao(); // Assuming dao is class member initialized in @BeforeEach

        // Path "CSV_RELATIVE_PATH" is a placeholder. For a real test, set up a file.
        // Using the temp file from setUp as an example, assuming it was copied from admin_transactions.csv
        // String testFileForDelete = testCsvPathForWrites.toString(); // If using @TempDir or similar setup

        // For this example, let's assume TEST_CSV_PATH is a valid, modifiable test file.
        // To make this test robust, you'd first ensure the transaction to delete exists.
        // For now, this is just a call demonstration.
        String orderToDelete = "4200057899202502250932735481"; // An example order number
        // dao.deleteTransaction(testFileForDelete, orderToDelete);
        // List<Transaction> transactions = dao.loadFromCSV(testFileForDelete);
        // Then assert that the transaction is no longer in the list.

        // The original lines:
        dao.deleteTransaction("CSV_RELATIVE_PATH", orderToDelete); // This path needs to be valid.
        List<Transaction> transactions = dao.loadFromCSV(CSV_PATH); // This path also needs to be valid and reflect the deletion.
        // Add assertions here.
    }

//    @Test
//    void testChangeInfo() throws IOException{
//        dao=new CsvTransactionDao();
//        // dao.changeInformation("TX123456","remarks","Test modify information",TEST_CSV_PATH); // "Test modify information"
//        // dao.changeInformation("TX123456","paymentAmount","116156",TEST_CSV_PATH);
//        // The changeInformation method is not standard in TransactionDao interface. Prefer using updateTransaction.
//        // If testing updateTransaction, it would look like:
//        // dao.updateTransaction(TEST_CSV_PATH, "TX123456", "remarks", "Test modify information");
//        // dao.updateTransaction(TEST_CSV_PATH, "TX123456", "paymentAmount", "116156.00"); // Ensure amount is a valid double string
//    }

}