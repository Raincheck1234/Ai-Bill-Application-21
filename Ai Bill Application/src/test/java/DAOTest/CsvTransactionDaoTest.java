package DAOTest;

import DAO.Impl.CsvTransactionDao;
import model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static Constants.ConfigConstants.CSV_PATH;
import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


class CsvTransactionDaoTest {
    private static final String TEST_ADMIN_CSV_PATH = "Ai Bill Application/src/test/resources/CSVForm/transactions/admin_transactions.csv"; //

    // 测试文件路径（根据实际结构调整）
    private static final String TEST_CSV_PATH = CSV_PATH;
    private static CsvTransactionDao dao;

    @Test
    void testLoadFromCSV_ValidFile_ReturnsTransactions() throws Exception {
        // Given
        CsvTransactionDao dao = new CsvTransactionDao();

        // When
        List<Transaction> transactions = dao.loadFromCSV(CSV_PATH);

        // 验证第一条记录的字段
        for (int i = 0; i < transactions.size(); i++) {
            System.out.println(transactions.get(i).getRemarks());
            System.out.println(transactions.get(i).getCommodity());
        }


    }
    @Test
    void testAddTransaction() throws IOException {
        dao=new CsvTransactionDao();
        Transaction newTx = new Transaction(
                "2025-03-09 10:00",
                "转账",
                "小明",
                "书籍",
                "支",
                99.99,
                "微信",
                "已完成",
                "TX123456",
                "M789012",
                "");

        dao.addTransaction("src/test/resources/001.csv", newTx);

        List<Transaction> transactions = dao.loadFromCSV(TEST_CSV_PATH);

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
        assertEquals(String.valueOf(5), transactions.size(), "Should load 5 transaction records"); // Assuming 5 rows plus header

        // Optional: Verify content of a specific row
        Transaction firstTx = transactions.get(0);
        assertEquals("公司A", firstTx.getCounterparty());
        assertEquals("三月工资", firstTx.getCommodity());
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

        String[] headers = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers).withTrim())) {

            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionTime(),
                        t.getTransactionType(),
                        t.getCounterparty(),
                        t.getCommodity(),
                        t.getInOut(),
                        String.format("¥%.2f", t.getPaymentAmount()),
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
        String tempFilePath = "Ai Bill Application/src/main/resources/CSVForm/transactions/test_add.csv";
        // Create an empty or initial test file
        createTestCsvFile(tempFilePath, List.of()); // Start with an empty file

        CsvTransactionDao testDao = new CsvTransactionDao(); // Or reuse the instance from BeforeEach if path is managed

        Transaction newTx = new Transaction(
                "2025/04/11 08:00:00", "测试类型", "测试对方", "测试商品", "收入",
                123.45, "测试方式", "测试状态", "TEST001", "MERCHANT001", "测试备注"
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
        dao=new CsvTransactionDao();

        dao.deleteTransaction("CSV_RELATIVE_PATH", "4200057899202502250932735481");

        List<Transaction> transactions = dao.loadFromCSV(CSV_PATH);
    }
//    @Test
//    void testChangeInfo() throws IOException{
//        dao=new CsvTransactionDao();
//        dao.changeInformation("TX123456","remarks","测试修改信息",TEST_CSV_PATH);
//        dao.changeInformation("TX123456","paymentAmount","116156",TEST_CSV_PATH);
//    }

}
