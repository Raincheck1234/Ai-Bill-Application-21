package Service;

import DAO.Impl.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;
import model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;

class TransactionServiceTest {
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
//        // 初始化 DAO 和 Service
//        CsvTransactionDao csvTransactionDao = new CsvTransactionDao();
//        transactionService = new TransactionServiceImpl(csvTransactionDao);
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

        // 设置搜索条件
        Transaction searchCriteria = new Transaction();
        searchCriteria.setCounterparty("支付宝");

        // 执行搜索操作
        List<Transaction> result = transactionService.searchTransaction(searchCriteria);

        // 验证搜索结果
        result.forEach(res -> System.out.println(res.getCommodity()));
    }


}