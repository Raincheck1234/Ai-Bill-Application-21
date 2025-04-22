package Service;

import DAO.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;
import model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;

class TransactionServiceTest {
    private TransactionService transactionService;
    private static final String TEST_CSV_PATH = "src/main/resources/CSVForm/0001.csv";

    @BeforeEach
    void setUp() {
        // 初始化 DAO 和 Service
        CsvTransactionDao csvTransactionDao = new CsvTransactionDao();
        transactionService = new TransactionServiceImpl(csvTransactionDao);
    }

    @Test
    void testAddTransaction() throws IOException {
        // 准备测试数据
        Transaction transaction = new Transaction(
                "2023-08-20 15:30:00", "转账", "李四", "虚拟商品", "支出", 500.0,
                "银行卡", "已完成", "T123456789", "M987654321", "测试"
        );

        // 执行添加操作
        transactionService.addTransaction(transaction);

        // 验证是否添加成功
        List<Transaction> transactions = transactionService.searchTransaction(new Transaction());
        assertFalse(transactions.isEmpty());
        assertEquals("T123456789", transactions.get(0).getOrderNumber());
    }

    @Test
    void testChangeTransaction() throws Exception {
        // 准备测试数据
        Transaction originalTransaction = new Transaction(
                "2023-08-20 15:30:00", "转账", "李四", "虚拟商品", "支出", 500.0,
                "银行卡", "已完成", "T123456789", "M987654321", "测试"
        );
        transactionService.addTransaction(originalTransaction);

        // 准备更新数据
        Transaction updatedTransaction = new Transaction(
                null, "充值", null, null, null, 0.0, "微信支付", null, "T123456789", null, "更新备注"
        );

        // 执行更新操作
        transactionService.changeTransaction(updatedTransaction);

        // 验证是否更新成功
        List<Transaction> transactions = transactionService.searchTransaction(new Transaction());
        assertEquals("充值", transactions.get(0).getTransactionType());
        assertEquals("微信支付", transactions.get(0).getPaymentMethod());
        assertEquals("更新备注", transactions.get(0).getRemarks());
    }

    @Test
    void testDeleteTransaction() throws Exception {
        // 准备测试数据
        Transaction transaction = new Transaction(
                "2023-08-20 15:30:00", "转账", "李四", "虚拟商品", "支出", 500.0,
                "银行卡", "已完成", "T123456789", "M987654321", "测试"
        );
        transactionService.addTransaction(transaction);

        // 执行删除操作
        boolean result = transactionService.deleteTransaction("T123456789");

        // 验证是否删除成功
        assertTrue(result);
        List<Transaction> transactions = transactionService.searchTransaction(new Transaction());
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testSearchTransaction() throws IOException {
        // 准备测试数据
        Transaction transaction1 = new Transaction(
                "2023-08-20 15:30:00", "转账", "李四", "虚拟商品", "支出", 500.0,
                "银行卡", "已完成", "T123456789", "M987654321", "测试"
        );
        Transaction transaction2 = new Transaction(
                "2023-08-21 10:00:00", "充值", "支付宝", "虚拟商品", "收入", 1000.0,
                "微信支付", "已完成", "T987654321", "M123456789", "测试"
        );
        transactionService.addTransaction(transaction1);
        transactionService.addTransaction(transaction2);

        // 设置搜索条件
        Transaction searchCriteria = new Transaction();
        searchCriteria.setCounterparty("支付宝");

        // 执行搜索操作
        List<Transaction> result = transactionService.searchTransaction(searchCriteria);

        // 验证搜索结果
        assertEquals(1, result.size());
        assertEquals("T987654321", result.get(0).getOrderNumber());
    }
}