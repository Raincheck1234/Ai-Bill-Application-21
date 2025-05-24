package Utils;

import DAO.Impl.CsvTransactionDao;
import DAO.TransactionDao;
import model.Transaction;
import Constants.CaffeineKeys;
import Constants.ConfigConstants; // For a sample file path

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

public class CacheManagerTest {

    private TransactionDao transactionDao;
    private Path tempFilePath;
    // Use a known existing file for setting up the temp file
    private final String sampleTransactionFilePath = "src/test/resources/CSVForm/transactions/admin_transactions.csv";


    @BeforeEach
    void setUp() throws IOException {
        transactionDao = new CsvTransactionDao();
        Path originalPath = Paths.get(sampleTransactionFilePath);
        tempFilePath = Files.createTempFile("cache_manager_test_", ".csv");
        Files.copy(originalPath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("CacheManagerTest: Set up with temp file: " + tempFilePath.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFilePath != null && Files.exists(tempFilePath)) {
            Files.delete(tempFilePath);
            System.out.println("CacheManagerTest: Deleted temp file: " + tempFilePath.toString());
        }
        // Clean up the cache for this file path to avoid interference between tests
        CacheManager.invalidateTransactionCache(tempFilePath.toString());
        // Optionally, if fileCaches map allows removal:
        // CacheManager.fileCaches.remove(tempFilePath.toString());
    }

    @Test
    void testGetTransactions_LoadsAndCaches() {
        System.out.println("CacheManagerTest: Running testGetTransactions_LoadsAndCaches...");
        try {
            System.out.println("CacheManagerTest: First call to getTransactions for: " + tempFilePath.toString());
            List<Transaction> transactions1 = CacheManager.getTransactions(tempFilePath.toString(), transactionDao);
            System.out.println("CacheManagerTest: Loaded " + transactions1.size() + " transactions (call 1).");

            System.out.println("CacheManagerTest: Second call to getTransactions for: " + tempFilePath.toString());
            List<Transaction> transactions2 = CacheManager.getTransactions(tempFilePath.toString(), transactionDao);
            System.out.println("CacheManagerTest: Loaded " + transactions2.size() + " transactions (call 2 - should be cached).");

            // Simple check: sizes should be the same.
            if (transactions1.size() == transactions2.size()) {
                System.out.println("CacheManagerTest: Sizes match, caching likely worked.");
            } else {
                System.err.println("CacheManagerTest: Sizes MISMATCH. Caching might have issues.");
            }

        } catch (Exception e) {
            System.err.println("CacheManagerTest (getTransactions): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CacheManagerTest: testGetTransactions_LoadsAndCaches finished.");
    }

    @Test
    void testPutAndInvalidateTransactions() {
        System.out.println("CacheManagerTest: Running testPutAndInvalidateTransactions...");
        try {
            // Get initial state
            List<Transaction> initialTransactions = CacheManager.getTransactions(tempFilePath.toString(), transactionDao);
            System.out.println("CacheManagerTest: Initial transactions count: " + initialTransactions.size());

            // Create a new list and put it into cache
            List<Transaction> manualList = new ArrayList<>();
            manualList.add(new Transaction("2024/04/01", "ManualPut", "MP_C", "MP_I", "支出", 1.0, "MP_P", "OK", "MP_ON", "MP_M", "Manual"));

            System.out.println("CacheManagerTest: Putting manual list with " + manualList.size() + " transaction(s) into cache for: " + tempFilePath.toString());
            CacheManager.putTransactions(tempFilePath.toString(), manualList, transactionDao);

            List<Transaction> afterPut = CacheManager.getTransactions(tempFilePath.toString(), transactionDao);
            System.out.println("CacheManagerTest: Transactions count after put: " + afterPut.size());
            if (afterPut.size() == manualList.size()) {
                System.out.println("CacheManagerTest: 'putTransactions' seems to have updated the cache.");
            } else {
                System.err.println("CacheManagerTest: 'putTransactions' did NOT update cache as expected.");
            }

            // Invalidate and get again (should reload from file, which still has original content)
            System.out.println("CacheManagerTest: Invalidating cache for: " + tempFilePath.toString());
            CacheManager.invalidateTransactionCache(tempFilePath.toString());

            List<Transaction> afterInvalidate = CacheManager.getTransactions(tempFilePath.toString(), transactionDao);
            System.out.println("CacheManagerTest: Transactions count after invalidate and get: " + afterInvalidate.size());
            if (afterInvalidate.size() == initialTransactions.size()) {
                System.out.println("CacheManagerTest: Cache invalidation and reload from original file content worked.");
            } else {
                System.err.println("CacheManagerTest: Cache invalidation or reload FAILED. Expected " + initialTransactions.size() + ", got " + afterInvalidate.size());
            }

        } catch (Exception e) {
            System.err.println("CacheManagerTest (putInvalidate): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CacheManagerTest: testPutAndInvalidateTransactions finished.");
    }
}