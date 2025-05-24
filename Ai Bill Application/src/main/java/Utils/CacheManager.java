package Utils;

import Constants.CaffeineKeys;
import DAO.Impl.CsvTransactionDao; // Assuming CsvTransactionDao is in DAO.Impl
import DAO.TransactionDao; // Assuming TransactionDao interface is in DAO
import model.Transaction;
import com.github.benmanes.caffeine.cache.Cache; // Use general Cache interface
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages Caffeine caches for different transaction data files (per user).
 * Uses the file path as the cache key.
 */
public class CacheManager {

    // Use a map to hold caches, keyed by file path
    // The value is the Caffeine LoadingCache for that specific file path
    private static final ConcurrentHashMap<String, LoadingCache<String, List<Transaction>>> fileCaches = new ConcurrentHashMap<>();

    // Define default cache parameters
    private static final int DEFAULT_MAX_SIZE = 1; // Only cache one instance (the list of transactions) per file
    private static final long DEFAULT_EXPIRE_AFTER_WRITE_MINUTES = 10; // Cache entry expires after 10 minutes
    private static final long DEFAULT_REFRESH_AFTER_WRITE_MINUTES = 1; // Refresh entry after 1 minute

    // Private constructor to prevent instantiation
    private CacheManager() {}

    /**
     * Gets or creates a LoadingCache for the specified transaction file path.
     * The cache loads List<Transaction> from the file using TransactionDao.
     *
     * @param filePath The path to the user's transaction CSV file.
     * @param transactionDao The TransactionDao instance to use for loading.
     * @return The LoadingCache instance for the given file path.
     */
    public static LoadingCache<String, List<Transaction>> getTransactionCache(String filePath, TransactionDao transactionDao) {
        // Use computeIfAbsent to get or create the cache atomically
        return fileCaches.computeIfAbsent(filePath, key -> {
            System.out.println("CacheManager: Creating new cache for file: " + filePath);
            // Create a new LoadingCache for this specific file path
            return Caffeine.newBuilder()
                    .maximumSize(DEFAULT_MAX_SIZE)
                    .expireAfterWrite(DEFAULT_EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                    .refreshAfterWrite(DEFAULT_REFRESH_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                    // Define the loader function: how to load data when cache is missed or refreshed
                    .build(cacheKey -> {
                        System.out.println("CacheManager: Loading transactions from file: " + filePath + " (Cache Miss/Refresh)");
                        try {
                            // The cacheKey here will likely be a constant like "transactions"
                            // We use the outer filePath variable to load from the correct file
                            return transactionDao.loadFromCSV(filePath);
                        } catch (IOException e) {
                            System.err.println("CacheManager: Error loading data for file " + filePath);
                            e.printStackTrace();
                            throw new RuntimeException("Error loading transactions from " + filePath, e); // Wrap IOException in RuntimeException for Caffeine loader
                        }
                    });
        });
    }

    /**
     * Invalidates the cache for a specific transaction file path.
     * @param filePath The path to the user's transaction CSV file.
     */
    public static void invalidateTransactionCache(String filePath) {
        LoadingCache<String, List<Transaction>> cache = fileCaches.get(filePath);
        if (cache != null) {
            System.out.println("CacheManager: Invalidating cache for file: " + filePath);
            // The cache key for List<Transaction> is likely a constant like "transactions"
            cache.invalidate(CaffeineKeys.TRANSACTION_CAFFEINE_KEY); // Invalidate the entry storing the transaction list
        }

    }

    /**
     * Gets the transaction list from the cache for the specified file path.
     * Loads data if not present or expired. Handles exceptions thrown by the loader.
     *
     * @param filePath The path to the user's transaction CSV file.
     * @param transactionDao The TransactionDao instance to use for loading if cache misses.
     * @return The list of transactions.
     * @throws Exception If an error occurs during loading (e.g., IOException).
     */
    public static List<Transaction> getTransactions(String filePath, TransactionDao transactionDao) throws Exception {
        LoadingCache<String, List<Transaction>> cache = getTransactionCache(filePath, transactionDao);
        // The cache key for the list of transactions from a specific file is a constant.
        // This constant key maps to the *entire list* of transactions for that file.
        return cache.get(CaffeineKeys.TRANSACTION_CAFFEINE_KEY);
    }

    /**
     * Manually puts a list of transactions into the cache for a specific file path.
     * This is useful after a write operation (add, delete, update) to refresh the cache.
     *
     * @param filePath The path to the user's transaction CSV file.
     * @param transactions The updated list of transactions.
     * @param transactionDao The TransactionDao instance (needed to get/create cache if not exists).
     */
    public static void putTransactions(String filePath, List<Transaction> transactions, TransactionDao transactionDao) {
        LoadingCache<String, List<Transaction>> cache = getTransactionCache(filePath, transactionDao);
        cache.put(CaffeineKeys.TRANSACTION_CAFFEINE_KEY, transactions);
        System.out.println("CacheManager: Manually updated cache for file: " + filePath);
    }

    /**
     * Shutdown any resources if necessary (though Caffeine typically manages its threads).
     */
    public static void shutdown() {
        // Caffeine cache doesn't require explicit shutdown in most cases
        // If using custom executors, they might need shutdown.
        System.out.println("CacheManager: Shutdown completed.");
    }
}