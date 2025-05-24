package DAO;

import model.Transaction;

import java.io.IOException;
import java.util.List;

/**
 * Interface for Data Access Object (DAO) operations related to Transactions.
 * Defines the contract for loading, adding, deleting, and updating transaction data.
 */
public interface TransactionDao {

    // Keep loadFromCSV - used by cache loader
    List<Transaction> loadFromCSV(String filePath) throws IOException;

    /**
     * Loads all transactions from the specified data source file.
     * @param filePath The path to the user's CSV file.
     * @return A list of all transactions.
     * @throws IOException If an I/O error occurs during loading.
     */
    List<Transaction> getAllTransactions(String filePath) throws IOException;


    /**
     * Adds a new transaction to the specified data source file.
     *
     * @param filePath The path to the user's CSV file.
     * @param transaction The new transaction to add.
     * @throws IOException If an I/O error occurs during saving.
     */
    void addTransaction(String filePath, Transaction transaction) throws IOException;

    /**
     * Deletes a transaction identified by its order number from the specified data source file.
     *
     * @param filePath The path to the user's CSV file.
     * @param orderNumber The unique order number of the transaction to delete.
     * @return true if a transaction was found and deleted, false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
    boolean deleteTransaction(String filePath, String orderNumber) throws IOException;

    /**
     * Updates a specific field of an existing transaction identified by its order number
     * in the specified data source file.
     *
     * @param filePath The path to the user's CSV file.
     * @param orderNumber The unique order number of the transaction to update.
     * @param fieldName   The name of the field to update (e.g., "transactionType", "paymentAmount").
     * @param newValue    The new value for the specified field.
     * @return true if the transaction was found and updated, false otherwise.
     * @throws IOException             If an I/O error occurs during loading or saving.
     * @throws NumberFormatException   If updating 'paymentAmount' and newValue is not a valid double.
     * @throws IllegalArgumentException If the fieldName is invalid.
     */
    boolean updateTransaction(String filePath, String orderNumber, String fieldName, String newValue) throws IOException;

    /**
     * Retrieves a transaction by its unique order number from the specified data source file.
     *
     * @param filePath The path to the user's CSV file.
     * @param orderNumber The unique order number.
     * @return The Transaction object if found, null otherwise.
     * @throws IOException If an I/O error occurs during loading.
     */
    Transaction getTransactionByOrderNumber(String filePath, String orderNumber) throws IOException;

    void writeTransactionsToCSV(String currentUserTransactionFilePath, List<Transaction> updatedList) throws IOException;

    // Remove the old methods without filePath parameter from the interface
    // List<Transaction> getAllTransactions() throws IOException; // Removed
    // void addTransaction(Transaction transaction) throws IOException; // Removed
    // boolean deleteTransaction(String orderNumber) throws IOException; // Removed
    // boolean updateTransaction(String orderNumber, String fieldName, String newValue) throws IOException; // Removed
    // Transaction getTransactionByOrderNumber(String orderNumber) throws IOException; // Removed
}