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

    // Remove the 'transactions' field and 'isLoad' flag, the cache/service layer will handle loading
    // private List<Transaction> transactions;
    // private boolean isLoad= false;

    // Keep the load method, it will be used by the cache loader
    @Override
    public List<Transaction> loadFromCSV(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        Path path = Paths.get(filePath);

        // Check if file exists and is not empty before attempting to read
        if (!Files.exists(path) || Files.size(path) == 0) {
            System.out.println("CSV file not found or is empty: " + filePath);
            return transactions; // Return empty list if file doesn't exist or is empty
        }

        // Use BOMInputStream and InputStreamReader with UTF-8
        try (Reader reader = new InputStreamReader(
                new BOMInputStream(Files.newInputStream(path)), // Use path here directly
                StandardCharsets.UTF_8)) {

            // *** Simplified Header Handling: Let CSVParser handle the header detection ***
            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()    // Tell parser to treat the first line as header
                    .withIgnoreHeaderCase(true)  // Ignore case for robustness
                    .withTrim(true);             // Trim fields

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                // After creating the parser with withFirstRecordAsHeader,
                // the header map should be available *if* a header was found.
                // Check if the required headers are present
                List<String> requiredHeaders = List.of(
                        "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)",
                        "支付方式", "当前状态", "交易单号", "商户单号", "备注"
                );

                // Now check the header map obtained *by the parser*
                Map<String, Integer> headerMap = csvParser.getHeaderMap();
                if (headerMap == null || !headerMap.keySet().containsAll(requiredHeaders)) {
                    // If headerMap is null (no header found) or incomplete
                    throw new IOException("Missing required headers in CSV file: " + requiredHeaders +
                            " Found: " + (headerMap == null ? "null" : headerMap.keySet()));
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


    // Keep parseRecord private as it's an internal helper
    private Transaction parseRecord(CSVRecord record) {
        // Safely get and trim values, handle potential missing columns gracefully if needed
        String amountStr = record.get("金额(元)").trim();
        double paymentAmount = 0.0;
        try {
            if (amountStr.startsWith("¥")) {
                amountStr = amountStr.substring(1);
            }
            paymentAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse payment amount '" + record.get("金额(元)") + "' at line " + record.getRecordNumber());
            // Keep paymentAmount as 0.0 or handle as an error depending on strictness
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Missing '金额(元)' column or empty value at line " + record.getRecordNumber());
        }


        return new Transaction(
                record.get("交易时间").trim(),
                record.get("交易类型").trim(),
                record.get("交易对方").trim(),
                record.get("商品").trim(),
                record.get("收/支").trim(),
                paymentAmount, // Use parsed amount
                record.get("支付方式").trim(),
                record.get("当前状态").trim(),
                record.get("交易单号").trim(),
                record.get("商户单号").trim(),
                record.get("备注").trim()
        );
    }

    // Implement DAO interface methods properly
    public List<Transaction> getAllTransactions() throws IOException {
        // This method is now handled by the service layer using the cache.
        // The DAO should focus on direct file operations.
        // This interface method might be redundant if service layer always uses cache.
        // For clarity, let's make it load from CSV directly, but the service will prefer cache.
        // Note: This might re-read the file even if cached. Service layer needs to manage this.
        // Alternatively, this method could be removed from the interface/DAO if only cache loader uses loadFromCSV.
        // Let's keep it for now, assuming it's a direct file read fallback.
        System.out.println("DAO: Calling getAllTransactions directly from file (consider using service/cache)");
        // We need a way to know *which* file here. This method signature is problematic for multi-user.
        // The interface needs filePath, or the DAO instance needs to be created per file.
        // Let's adjust the interface/DAO to be file-specific or pass path to methods.
        // Option 1: DAO instance per file.
        // Option 2: Add filePath parameter to all relevant interface methods.
        // Option 2 seems more flexible for a single CsvTransactionDao class.
        // Let's add filePath to interface methods and implement here.

        // *** Decision: Modify TransactionDao interface to include filePath ***
        throw new UnsupportedOperationException("This method signature is not suitable for multi-user. Use the overloaded method with filePath.");
    }

    // Adding filePath parameter to relevant interface methods definition (will update interface next)
    // This implementation will be part of the updated DAO after interface change.
    public List<Transaction> getAllTransactions(String filePath) throws IOException {
        return loadFromCSV(filePath); // Simple implementation using the existing load method
    }


    // Add transaction
    public void addTransaction(Transaction newTransaction) throws IOException {
        throw new UnsupportedOperationException("This method signature is not suitable for multi-user. Use the overloaded method with filePath.");
    }

    public void addTransaction(String filePath, Transaction newTransaction) throws IOException {
        // Ensure the directory exists
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        boolean fileExists = Files.exists(path) && Files.size(path) > 0;

        // Define the header based on your CSV structure
        String[] headers = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};

        // Use StandardOpenOption.CREATE and StandardOpenOption.APPEND
        // If the file doesn't exist, CREATE will create it. If it exists, APPEND will add to the end.
        // We need to handle writing the header only if the file is new or empty.
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            // Check if header needs to be written. A simple way is to check file size before opening in APPEND mode.
            // However, opening in CREATE/APPEND and then checking size *after* opening might not work as expected if the file is created.
            // A better approach is to check size *before* getting the writer or read the first line after opening if needed.
            // The Apache CSVPrinter can handle writing headers IF the file is new.
            // Let's adapt the existing logic slightly. Check exists and non-empty *before* opening.

            // Re-check file state after potential creation by StandardOpenOption.CREATE
            boolean fileWasEmptyBeforeAppend = !fileExists; // Or check if file size is 0 after creation if needed

            // Configure CSV format - with header if file is new/empty, without if appending
            CSVFormat format;
            if (fileWasEmptyBeforeAppend) {
                format = CSVFormat.DEFAULT.withHeader(headers).withTrim();
            } else {
                format = CSVFormat.DEFAULT.withTrim(); // Assume header is already there
            }

            // Create CSVPrinter
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
                csvPrinter.printRecord(
                        newTransaction.getTransactionTime(),
                        newTransaction.getTransactionType(),
                        newTransaction.getCounterparty(),
                        newTransaction.getCommodity(),
                        newTransaction.getInOut(),
                        // Format amount with ¥ sign and two decimal places
                        String.format("¥%.2f", newTransaction.getPaymentAmount()),
                        newTransaction.getPaymentMethod(),
                        newTransaction.getCurrentStatus(),
                        newTransaction.getOrderNumber(),
                        newTransaction.getMerchantNumber(),
                        newTransaction.getRemarks()
                );
                // No need to flush immediately, writer will be closed by try-with-resources.
                // csvPrinter.flush();
            }
            System.out.println("Added transaction to " + filePath);
        } catch (IOException e) {
            System.err.println("Error adding transaction to CSV: " + filePath);
            e.printStackTrace();
            throw e; // Re-throw
        }
    }


    // Delete transaction by order number
    public boolean deleteTransaction(String orderNumber) throws IOException {
        throw new UnsupportedOperationException("This method signature is not suitable for multi-user. Use the overloaded method with filePath.");
    }

    public boolean deleteTransaction(String filePath, String orderNumber) throws IOException {
        // Load all transactions first
        List<Transaction> allTransactions = loadFromCSV(filePath);

        // Filter out the transaction to be deleted
        List<Transaction> updatedTransactions = allTransactions.stream()
                .filter(t -> !t.getOrderNumber().trim().equals(orderNumber.trim()))
                .collect(Collectors.toList());

        // Check if any transaction was actually removed
        boolean deleted = allTransactions.size() > updatedTransactions.size();

        if (deleted) {
            // Write the remaining transactions back to the CSV file
            writeTransactionsToCSV(filePath, updatedTransactions);
            System.out.println("Deleted transaction with order number " + orderNumber + " from " + filePath);
        } else {
            System.out.println("Transaction with order number " + orderNumber + " not found in " + filePath);
        }

        return deleted;
    }


    // Update a specific field (implementing the interface method)
    public boolean updateTransaction(String orderNumber, String fieldName, String newValue) throws IOException {
        // This interface method needs a filePath parameter to be useful in a multi-user context.
        // Let's add an overloaded method that includes filePath.
        throw new UnsupportedOperationException("This method signature is not suitable for multi-user. Use the overloaded method with filePath.");
    }

    public boolean updateTransaction(String filePath, String orderNumber, String fieldName, String newValue) throws IOException {
        // Load all transactions
        List<Transaction> allTransactions = loadFromCSV(filePath);

        // Find the transaction by order number
        Optional<Transaction> transactionToUpdateOpt = allTransactions.stream()
                .filter(t -> t.getOrderNumber().trim().equals(orderNumber.trim()))
                .findFirst();

        if (!transactionToUpdateOpt.isPresent()) {
            System.out.println("Transaction with order number " + orderNumber + " not found for update in " + filePath);
            return false; // Transaction not found
        }

        Transaction transactionToUpdate = transactionToUpdateOpt.get();
        boolean updated = false;

        // Use reflection or a switch/if-else block to update the specific field
        // A switch is more explicit and safer than reflection here.
        switch (fieldName) {
            case "transactionTime": transactionToUpdate.setTransactionTime(newValue); updated = true; break;
            case "transactionType": transactionToUpdate.setTransactionType(newValue); updated = true; break;
            case "counterparty": transactionToUpdate.setCounterparty(newValue); updated = true; break;
            case "commodity": transactionToUpdate.setCommodity(newValue); updated = true; break;
            case "inOut": transactionToUpdate.setInOut(newValue); updated = true; break;
            case "paymentAmount":
                try {
                    transactionToUpdate.setPaymentAmount(Double.parseDouble(newValue));
                    updated = true;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for paymentAmount update: " + newValue);
                    throw new NumberFormatException("Invalid number format for paymentAmount: " + newValue);
                }
                break;
            case "paymentMethod": transactionToUpdate.setPaymentMethod(newValue); updated = true; break;
            case "currentStatus": transactionToUpdate.setCurrentStatus(newValue); updated = true; break;
            case "orderNumber": transactionToUpdate.setOrderNumber(newValue); updated = true; break; // Caution: Updating ID can be tricky
            case "merchantNumber": transactionToUpdate.setMerchantNumber(newValue); updated = true; break;
            case "remarks": transactionToUpdate.setRemarks(newValue); updated = true; break;
            default:
                System.err.println("Invalid field name for update: " + fieldName);
                throw new IllegalArgumentException("Invalid field name: " + fieldName);
        }

        if (updated) {
            // Write the modified list back to the CSV file
            writeTransactionsToCSV(filePath, allTransactions);
            System.out.println("Updated transaction with order number " + orderNumber + " in " + filePath + " field: " + fieldName);
        }

        return updated;
    }


    // Keep writeTransactionsToCSV, ensure it uses the filePath parameter correctly
    // This method seems OK as it already accepts filePath.
    public void writeTransactionsToCSV(String filePath, List<Transaction> transactions) throws IOException {
        // Ensure the directory exists
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        File targetFile = path.toFile();
        // Create temporary file in the same directory
        File tempFile = File.createTempFile("transaction_temp", ".csv", targetFile.getParentFile());

        // Define the header explicitly
        String[] headers = {"交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"};

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers).withTrim())) { // Always write header for overwrite

            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionTime(),
                        t.getTransactionType(),
                        t.getCounterparty(),
                        t.getCommodity(),
                        t.getInOut(),
                        String.format("¥%.2f", t.getPaymentAmount()), // Format amount
                        t.getPaymentMethod(),
                        t.getCurrentStatus(),
                        t.getOrderNumber(),
                        t.getMerchantNumber(),
                        t.getRemarks()
                );
            }
            // csvPrinter.flush(); // Auto-flushed on close
        } catch (IOException e) {
            tempFile.delete(); // Clean up temp file on failure
            System.err.println("Error writing transactions to temporary CSV file: " + tempFile.toPath());
            e.printStackTrace();
            throw e; // Re-throw
        }

        // Atomic replacement of the original file
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Atomically replaced " + filePath + " with updated data.");
        } catch (IOException e) {
            System.err.println("Failed to atomically replace original file: " + targetFile.toPath() + " with " + tempFile.toPath());
            tempFile.delete(); // Clean up temp file
            e.printStackTrace();
            throw e; // Re-throw
        }
    }

    // Implement getTransactionByOrderNumber from the interface
    public Transaction getTransactionByOrderNumber(String orderNumber) throws IOException {
        throw new UnsupportedOperationException("This method signature is not suitable for multi-user. Use the overloaded method with filePath.");
    }

    public Transaction getTransactionByOrderNumber(String filePath, String orderNumber) throws IOException {
        // Load all transactions
        List<Transaction> allTransactions = loadFromCSV(filePath);

        // Find the transaction by order number
        Optional<Transaction> transactionOpt = allTransactions.stream()
                .filter(t -> t.getOrderNumber().trim().equals(orderNumber.trim()))
                .findFirst();

        return transactionOpt.orElse(null); // Return Transaction object or null
    }


    // Remove or update these old methods
    // private CSVFormat getCsvFormatWithHeader(String filePath) throws IOException { ... } // No longer needed with explicit headers
    // private CSVFormat getCsvFormatWithoutHeader() { ... } // No longer needed with explicit headers
    // boolean changeInformation(String orderNumber, String head, String value,String path) throws IOException // This is similar to updateTransaction, prefer the standard update method
    // Let's remove changeInformation and update the service to use updateTransaction

    // Removed changeInformation method based on the plan to use updateTransaction instead.
    // Removed getCsvFormatWithHeader/WithoutHeader as we now use explicit headers in writeTransactionsToCSV.
}