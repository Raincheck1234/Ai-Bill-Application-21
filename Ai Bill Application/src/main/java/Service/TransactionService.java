package Service;

import model.MonthlySummary;
import model.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TransactionService {

    /**
     * Gets all transactions for the current user.
     * @return List of all transactions.
     * @throws Exception If data retrieval fails (e.g., IO error, cache issue).
     */
    List<Transaction> getAllTransactions() throws Exception; // Added this method


    /**
     * 新增交易
     * @param transaction
     */
    void addTransaction(Transaction transaction) throws IOException;

    /**
     * 修改交易
     * @param transaction
     * @throws Exception If modification fails.
     */
    void changeTransaction(Transaction transaction) throws Exception;

    /**
     * 根据订单号删除交易
     * @param orderNumber
     * @return true if deletion was successful, false if transaction not found.
     * @throws Exception If deletion fails (e.g., IO error).
     */
    boolean deleteTransaction(String orderNumber) throws Exception; // Changed return type to boolean

    /**
     * 根据用户输入信息查询交易
     * @param transaction Search criteria.
     * @return List of matched transactions.
     */
    List<Transaction> searchTransaction(Transaction transaction);

    /**
     * Imports transactions from a given CSV file path into the current user's transactions.
     *
     * @param userFilePath The file path for the current user's transactions (target).
     * @param importFilePath The file path of the CSV to import from (source).
     * @return The number of transactions successfully imported.
     * @throws Exception If an error occurs during reading, parsing, or saving.
     */
    int importTransactionsFromCsv(String userFilePath, String importFilePath) throws Exception; // Added this method


    /**
     * Aggregates transactions for the current user by month and standard category.
     *
     * @return A map where keys are month identifiers (e.g., "YYYY-MM") and values are MonthlySummary objects.
     * @throws Exception If an error occurs during data retrieval.
     */
    Map<String, MonthlySummary> getMonthlyTransactionSummary() throws Exception; // Added this method
}