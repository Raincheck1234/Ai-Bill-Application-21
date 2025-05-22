package Service;

import DAO.Impl.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;
import Utils.CacheUtil;
import model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static Constants.CaffeineKeys.TRANSACTION_CAFFEINE_KEY;

public class CacheTest {

//    TransactionServiceImpl transactionService = new TransactionServiceImpl(new CsvTransactionDao());
//
//    @Test
//    void DeleteCache(){
//        CacheUtil<String, List<Transaction>, Exception> cache = transactionService.cache;
//        cache.invalidate(TRANSACTION_CAFFEINE_KEY);
//    }

}
