package Service;

import DAO.Impl.CsvTransactionDao;
import model.Transaction;
import org.junit.jupiter.api.Test;
import Service.AIservice.AITransactionService;

import java.io.IOException;
import java.util.List;

public class AiFunctionTest {

    @Test
    public void testAnalyzeTransactions() throws IOException {

        String filePath = "src/test/resources/sample_transactions.csv";
        CsvTransactionDao transactionDao = new CsvTransactionDao();

        List<Transaction> transactions = transactionDao.loadFromCSV(filePath);

        // 实例化服务类
        AITransactionService service = new AITransactionService();

        // 调用合并格式化函数
        List<String> result = service.formatTransactions(transactions,"2025/03/29","2025/04/10");

        // 输出结果
        result.forEach(System.out::println);
    }




}