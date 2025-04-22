package DAOTest;

import DAO.CsvTransactionDao;
import model.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static Constants.ConfigConstants.CSV_PATH;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class CsvTransactionDaoTest {
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

    @Test
    void testDeleteTransaction() throws IOException {
        dao=new CsvTransactionDao();

        dao.deleteTransaction("CSV_RELATIVE_PATH", "4200057899202502250932735481");

        List<Transaction> transactions = dao.loadFromCSV(CSV_PATH);
    }
    @Test
    void testChangeInfo() throws IOException{
        dao=new CsvTransactionDao();
        dao.changeInformation("TX123456","remarks","测试修改信息",TEST_CSV_PATH);
        dao.changeInformation("TX123456","paymentAmount","116156",TEST_CSV_PATH);
    }

}
