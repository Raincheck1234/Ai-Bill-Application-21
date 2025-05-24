package DAO.Impl; // Changed package

import Constants.ConfigConstants;
import DAO.TransactionDao; // Implement the interface
import model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Using Optional for getTransactionByOrderNumber
import java.util.stream.Collectors;


public class CsvTransactionDao implements TransactionDao { // Implement TransactionDao interface

    @Override
    public List<Transaction> loadFromCSV(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path) || Files.size(path) == 0) {
            System.out.println("CSV file not found or is empty: " + filePath);
            return transactions;
        }

        try (Reader reader = new InputStreamReader(
                new BOMInputStream(Files.newInputStream(path)),
                StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase(true)
                    .withTrim(true);

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                // Define expected English headers
                List<String> requiredHeaders = List.of(
                        "Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)",
                        "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks"
                );

                Map<String, Integer> headerMap = csvParser.getHeaderMap();
                if (headerMap == null || !headerMap.keySet().containsAll(requiredHeaders)) {
                    throw new IOException("Missing required headers in CSV file. Expected: " + requiredHeaders +
                            " Found: " + (headerMap == null ? "No headers found by parser" : headerMap.keySet()));
                }
                System.out.println("Successfully identified headers: " + headerMap.keySet() + " in file: " + filePath);

                for (CSVRecord record : csvParser) {
                    try {
                        transactions.add(parseRecord(record));
                    } catch (Exception e) {
                        System.err.println("Skipping malformed record at line " + record.getRecordNumber() + ": " + record.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading CSV file: " + filePath);
            e.printStackTrace();
            throw e;
        }
        System.out.println("Successfully loaded " + transactions.size() + " records from " + filePath);
        return transactions;
    }

    private Transaction parseRecord(CSVRecord record) {
        // Now using English header names to get values from the record
        String amountStr = record.get("Amount(CNY)").trim();
        double paymentAmount = 0.0;
        try {
            if (amountStr.startsWith("¥") || amountStr.startsWith("$")) { // Allow for currency symbols
                amountStr = amountStr.substring(1);
            }
            paymentAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse payment amount '" + record.get("Amount(CNY)") + "' at line " + record.getRecordNumber());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Missing 'Amount(CNY)' column or empty value at line " + record.getRecordNumber());
        }

        // Expect English values for "In/Out" from CSV or standardize them here if necessary
        // For now, assuming the CSV provides "Income" or "Expense" directly for "In/Out"
        String inOut = record.get("In/Out").trim();
        // Example standardization if CSV might still have Chinese:
        // if (inOut.equals("收入") || inOut.equals("收")) {
        //     inOut = "Income";
        // } else if (inOut.equals("支出") || inOut.equals("支")) {
        //     inOut = "Expense";
        // }

        return new Transaction(
                record.get("Transaction Time").trim(),
                record.get("Transaction Type").trim(),
                record.get("Counterparty").trim(),
                record.get("Commodity").trim(),
                inOut, // Use the processed/standardized inOut
                paymentAmount,
                record.get("Payment Method").trim(),
                record.get("Current Status").trim(),
                record.get("Order Number").trim(),
                record.get("Merchant Number").trim(),
                record.get("Remarks").trim()
        );
    }

    @Override
    public List<Transaction> getAllTransactions(String filePath) throws IOException {
        return loadFromCSV(filePath);
    }

    @Override
    public void addTransaction(String filePath, Transaction newTransaction) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        boolean fileExists = Files.exists(path) && Files.size(path) > 0;

        // Use English headers for writing
        String[] headers = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks"};

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            CSVFormat format;
            if (!fileExists) { // If file did not exist or was empty before this operation
                format = CSVFormat.DEFAULT.withHeader(headers).withTrim();
            } else {
                format = CSVFormat.DEFAULT.withTrim();
            }

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
                csvPrinter.printRecord(
                        newTransaction.getTransactionTime(),
                        newTransaction.getTransactionType(),
                        newTransaction.getCounterparty(),
                        newTransaction.getCommodity(),
                        newTransaction.getInOut(), // Expecting this to be "Income" or "Expense"
                        String.format("¥%.2f", newTransaction.getPaymentAmount()), // Or use "CNY" prefix if preferred
                        newTransaction.getPaymentMethod(),
                        newTransaction.getCurrentStatus(),
                        newTransaction.getOrderNumber(),
                        newTransaction.getMerchantNumber(),
                        newTransaction.getRemarks()
                );
            }
            System.out.println("Added transaction to " + filePath);
        } catch (IOException e) {
            System.err.println("Error adding transaction to CSV: " + filePath);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean deleteTransaction(String filePath, String orderNumber) throws IOException {
        List<Transaction> allTransactions = loadFromCSV(filePath);
        List<Transaction> updatedTransactions = allTransactions.stream()
                .filter(t -> !t.getOrderNumber().trim().equals(orderNumber.trim()))
                .collect(Collectors.toList());

        boolean deleted = allTransactions.size() > updatedTransactions.size();
        if (deleted) {
            writeTransactionsToCSV(filePath, updatedTransactions);
            System.out.println("Deleted transaction with order number " + orderNumber + " from " + filePath);
        } else {
            System.out.println("Transaction with order number " + orderNumber + " not found in " + filePath);
        }
        return deleted;
    }

    @Override
    public boolean updateTransaction(String filePath, String orderNumber, String fieldName, String newValue) throws IOException {
        List<Transaction> allTransactions = loadFromCSV(filePath);
        Optional<Transaction> transactionToUpdateOpt = allTransactions.stream()
                .filter(t -> t.getOrderNumber().trim().equals(orderNumber.trim()))
                .findFirst();

        if (!transactionToUpdateOpt.isPresent()) {
            System.out.println("Transaction with order number " + orderNumber + " not found for update in " + filePath);
            return false;
        }

        Transaction transactionToUpdate = transactionToUpdateOpt.get();
        boolean updated = false;

        // Assuming fieldName matches the English property names of Transaction class
        switch (fieldName) {
            case "transactionTime":
                transactionToUpdate.setTransactionTime(newValue);
                updated = true;
                break;
            case "transactionType":
                transactionToUpdate.setTransactionType(newValue);
                updated = true;
                break;
            case "counterparty":
                transactionToUpdate.setCounterparty(newValue);
                updated = true;
                break;
            case "commodity":
                transactionToUpdate.setCommodity(newValue);
                updated = true;
                break;
            case "inOut": // Ensure newValue is "Income" or "Expense"
                if ("Income".equalsIgnoreCase(newValue) || "Expense".equalsIgnoreCase(newValue)) {
                    transactionToUpdate.setInOut(newValue);
                    updated = true;
                } else {
                    System.err.println("Invalid value for In/Out update: " + newValue + ". Must be 'Income' or 'Expense'.");
                }
                break;
            case "paymentAmount":
                try {
                    if (newValue.startsWith("¥") || newValue.startsWith("$")) {
                        newValue = newValue.substring(1);
                    }
                    transactionToUpdate.setPaymentAmount(Double.parseDouble(newValue));
                    updated = true;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for paymentAmount update: " + newValue);
                    throw new NumberFormatException("Invalid number format for paymentAmount: " + newValue);
                }
                break;
            case "paymentMethod":
                transactionToUpdate.setPaymentMethod(newValue);
                updated = true;
                break;
            case "currentStatus":
                transactionToUpdate.setCurrentStatus(newValue);
                updated = true;
                break;
            case "orderNumber":
                transactionToUpdate.setOrderNumber(newValue);
                updated = true;
                break;
            case "merchantNumber":
                transactionToUpdate.setMerchantNumber(newValue);
                updated = true;
                break;
            case "remarks":
                transactionToUpdate.setRemarks(newValue);
                updated = true;
                break;
            default:
                System.err.println("Invalid field name for update: " + fieldName);
                throw new IllegalArgumentException("Invalid field name: " + fieldName);
        }

        if (updated) {
            writeTransactionsToCSV(filePath, allTransactions);
            System.out.println("Updated transaction with order number " + orderNumber + " in " + filePath + " field: " + fieldName);
        }
        return updated;
    }

    @Override
    public void writeTransactionsToCSV(String filePath, List<Transaction> transactions) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        File targetFile = path.toFile();
        File tempFile = File.createTempFile("transaction_temp_", ".csv", targetFile.getParentFile());

        // Use English headers for writing
        String[] headers = {"Transaction Time", "Transaction Type", "Counterparty", "Commodity", "In/Out", "Amount(CNY)", "Payment Method", "Current Status", "Order Number", "Merchant Number", "Remarks"};

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers).withTrim())) {
            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionTime(),
                        t.getTransactionType(),
                        t.getCounterparty(),
                        t.getCommodity(),
                        t.getInOut(), // Expecting "Income" or "Expense"
                        String.format("¥%.2f", t.getPaymentAmount()), // Or "CNY" prefix
                        t.getPaymentMethod(),
                        t.getCurrentStatus(),
                        t.getOrderNumber(),
                        t.getMerchantNumber(),
                        t.getRemarks()
                );
            }
        } catch (IOException e) {
            if (tempFile.exists()) tempFile.delete();
            System.err.println("Error writing transactions to temporary CSV file: " + tempFile.toPath());
            e.printStackTrace();
            throw e;
        }

        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Atomically replaced " + filePath + " with updated data.");
        } catch (IOException e) {
            System.err.println("Failed to atomically replace original file: " + targetFile.toPath() + " with " + tempFile.toPath());
            if (tempFile.exists()) tempFile.delete();
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Transaction getTransactionByOrderNumber(String filePath, String orderNumber) throws IOException {
        List<Transaction> allTransactions = loadFromCSV(filePath);
        Optional<Transaction> transactionOpt = allTransactions.stream()
                .filter(t -> t.getOrderNumber().trim().equals(orderNumber.trim()))
                .findFirst();
        return transactionOpt.orElse(null);
    }
}