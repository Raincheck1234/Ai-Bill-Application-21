package DAO;

import model.Transaction;

import java.io.IOException;
import java.util.List;

/**
 * Interface for Data Access Object (DAO) operations related to Transactions.
 * Defines the contract for loading, adding, deleting, and updating transaction data.
 */
public interface TransactionDao {

    /**
     * Loads all transactions from the configured data source.
     * @param filePath The CSV file in the filepath directory.
     * @return A list of all transactions.
     * @throws IOException If an I/O error occurs during loading.
     */
    List<Transaction> loadFromCSV(String filePath) throws IOException;

    /**
     * Adds a new transaction to the data source.
     *
     * @param newTransaction The new transaction to add.
     * @param filePath The CSV file in the filepath directory.
     * @throws IOException If an I/O error occurs during saving.
     */
     void addTransaction(String filePath, Transaction newTransaction) throws IOException;

    /**
     * Deletes a transaction identified by its order number.
     *
     * @param orderNumber The unique order number of the transaction to delete.
     * @param filePath The CSV file in the filepath directory.
     * @return true if a transaction was found and deleted, false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
    boolean deleteTransaction(String filePath, String orderNumber) throws IOException;

    /**
     * Change one attribute of a transaction by its order number.
     *
     * @param orderNumber The unique order number of the transaction to be changed.
     * @param path The CSV file in the filepath directory.
     * @return true if a transaction was changed  false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
     boolean changeInformation(String orderNumber, String head, String value,String path) throws IOException;

    /**
     * Change one attribute of a transaction by its order number.
     *
     * @param filePath The CSV file in the filepath directory.
     * @param transactions The unique order number of the transaction to be changed.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
     void writeTransactionsToCSV(String filePath, List<Transaction> transactions) throws IOException;

}