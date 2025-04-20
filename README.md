## Project Overview
This module is a transaction record management 
system in the Service layer of the project that connects 
with the Dao layer that provides CRUD (Create, Read, Update
, Delete) functionality and data processing for
transaction data. Core features include transaction 
creation, modification, deletion, and flexible querying,
all accessible through standardized interfaces.

## Structure and Function
Swing GUI (Controller) <- Service <- DAO <-data(CSV/JSON)<-model

##  Model 

### Overview

This Java class, located in the `model` package, serves 
as a Plain Old Java Object (POJO) or data model to 
represent a single financial transaction record. It 
encapsulates all the standard details associated with 
a transaction, such as its time, type, amount, parties 
involved, status, and unique identifiers.

This class is typically used in conjunction with Data 
Access Objects (DAOs) like `TransactionDao` and
`CsvTransactionDao` to load, save, and manipulate 
transaction data from a persistent store (e.g., a CSV 
file or database).

### Package

`model`

### Fields

The `Transaction` class contains the following fields:

* `transactionTime` (String): The date and time when the transaction occurred (e.g., "2023-10-27 15:30:00").
* `transactionType` (String): The category or type of the transaction (e.g., "在线支付" - Online Payment, "转账" - Transfer, "退款" - Refund).
* `counterparty` (String): The name or identifier of the other party involved in the transaction.
* `commodity` (String): A description of the goods, service, or reason for the transaction (e.g., "Coffee", "Salary", "Online Course Fee").
* `inOut` (String): Indicates the direction of the money flow relative to the account holder. Typically:
* `paymentAmount` (double): The monetary value of the transaction. The currency unit (e.g., Yuan) is implied by the context where this class is used (often indicated by header names like "金额(元)" in associated CSV files).
* `paymentMethod` (String): The method used for the payment (e.g., "支付宝" - Alipay, "微信支付" - WeChat Pay, "银行卡" - Bank Card, "现金" - Cash).
* `currentStatus` (String): The current processing status of the transaction (e.g., "交易成功" - Transaction Successful, "等待付款" - Awaiting Payment, "已关闭" - Closed, "退款成功" - Refund Successful).
* `orderNumber` (String): A unique identifier assigned to this specific transaction instance by the payment system or application. Often used as a primary key.
* `merchantNumber` (String): An order number or identifier associated with the merchant's system, often provided for reconciliation purposes. May be empty or null if not applicable.
* `remarks` (String): An optional field for any additional notes, comments, or user-provided descriptions about the transaction.

### Constructors

* **`public Transaction()`**
    * Default no-argument constructor. Creates an empty `Transaction` object with all fields initialized to their default values (null for String, 0.0 for double).

* **`public Transaction(String transactionTime, String transactionType, ..., String remarks)`**
    * Parameterized constructor that accepts values for all fields. Allows for quick instantiation of a fully populated `Transaction` object.

### Methods

The class provides standard public **getter** (`get...()`) and **setter** (`set...()`) methods for all its private fields. These methods allow controlled access and modification of the transaction's attributes from other parts of the application.

* `getTransactionTime()` / `setTransactionTime(String time)`
* `getTransactionType()` / `setTransactionType(String type)`
* `getCounterparty()` / `setCounterparty(String counterparty)`
* `getCommodity()` / `setCommodity(String commodity)`
* `getInOut()` / `setInOut(String inOut)`
* `getPaymentAmount()` / `setPaymentAmount(double amount)`
* `getPaymentMethod()` / `setPaymentMethod(String method)`
* `getCurrentStatus()` / `setCurrentStatus(String status)`
* `getOrderNumber()` / `setOrderNumber(String orderNumber)`
* `getMerchantNumber()` / `setMerchantNumber(String merchantNumber)`
* `getRemarks()` / `setRemarks(String remarks)`

 
### Service (Business logic)
- Responsibilities:
Transaction CRUD operations, calling the DAO layer to read and write local files,
User-defined classification logic (e.g. modifying transaction categories)
  (3) Operations:

Add Transaction:

Transaction t = new Transaction();
// Set field values
service.addTransaction(t);

Modify Transaction:

Transaction update = new Transaction();
update.setOrderNumber("ORD123");
// Set fields to update
service.changeTransaction(update);

Query Transactions:

Transaction criteria = new Transaction();
criteria.setCounterparty("Alipay");
List<Transaction> results = service.searchTransaction(criteria);
Code structure
   Directory Layout:
   src/main/java/
   └── Service/
   ├── Impl/
   │   └── TransactionServiceImpl.java  # Service implementation
   └── TransactionService.java          # Service interface
   TransactionService.java:
   Defines the service interface with business-agnostic method signatures, enforcing decoupling 
   between business logic and implementation details.
   TransactionServiceImpl.java:
   Implements the concrete business logic while depending only on the interface-defined contract, maintaining loose coupling with the data access layer through dependency injection.


- AIService (Intelligent Analytics) Responsibilities: Generate budget recommendations based
on historical data, and detect cost-cutting recommendations for seasonal spending in China
(e.g. Spring Festival) (e.g. marking high-frequency small spending)


- **`AITransactionService`**

The central class that performs the full AI-driven transaction analysis. It:

- Loads transaction data from a CSV file via `CsvTransactionDao`.
- Filters transactions by user-specified time range (`startTimeStr`, `endTimeStr`).
- Aggregates data by counterparty (income/spending, transaction counts).
- Formats the result into concise summaries sent to the AI.
- Sends user questions and formatted data to the Volcengine Ark AI API.
- Returns a response within **400 characters** for efficient display.

Key methods:
- `analyzeTransactions(String userRequest, String filePath, String startTimeStr, String endTimeStr)`
  → Returns an AI-generated analysis result string.
- `runAiInThread(...)`
  → Runs the AI logic in a background thread (single-threaded executor).
- `formatTransactions(...)`
  → Groups transactions and summarizes them by counterparty and time range.
- `parseDateTime(...)`
  → Supports flexible input formats like `yyyy/MM/dd`, `yyyy/MM/dd HH:mm`, and more. - **`AIAnalyzerThread`** A `Runnable` implementation for threading scenarios (especially GUI usage). It calls `AITransactionService.analyzeTransactions(...)` and prints the result. UI developers can replace the console output with GUI updates using `SwingUtilities.invokeLater()`. --- ### Tests (`test/java/Service/AIservice/`) - **`AIserviceTest`** Unit tests for `AITransactionService`, verifying: - Correct loading and filtering of transactions. - Accurate AI response generation and formatting. - Graceful handling of edge cases (e.g., no transactions in range, invalid time formats). - **`AIAnalyzerThreadTest`** Validates threading logic: - Ensures `AIAnalyzerThread` executes `Runnable` correctly. - Demonstrates clean and concurrent AI analysis flow. - Simulates integration with a GUI environment. - **`AiFunctionTest`** Utility-level tests: - Ensures proper CSV parsing and data transformation. - Verifies output formatting before sending to the AI. - Tests various time formats and edge case behaviors

## DAO (File Storage)
- Responsibilities: Save transaction data in CSV or JSON format and provide interfaces for adding,
  deleting, and modifying in-memory data
### CsvTransactionDao Class

#### Package

`DAO`

#### Dependencies

* **`model.Transaction`**: The data model class representing a single transaction.
* **`DAO.TransactionDao`**: The interface this class is intended to implement (though the implementation is incomplete).
* **Apache Commons CSV (`org.apache.commons.csv.*`)**: Used for robust parsing and writing of CSV data.
* **Apache Commons IO (`org.apache.commons.io.input.BOMInputStream`)**: Used specifically to handle potential Byte Order Marks (BOM) often found in UTF-8 encoded files, particularly those edited in Windows.
* **Java Standard Libraries**: `java.io.*`, `java.nio.*`.

#### Key Features & Implementation Details

* **CSV Loading (`loadFromCSV`)**:
  * Reads data from a specified CSV file path.
  * Expects UTF-8 encoding.
  * Handles potential Byte Order Mark (BOM) at the beginning of the file.
  * Uses the first row of the CSV as the header to map columns to `Transaction` fields. Assumes specific header names (e.g., "交易时间", "交易类型", "金额(元)").
  * Parses transaction records into a `List<Transaction>`.
  * Includes basic caching using an internal `transactions` list and an `isLoad` flag to avoid redundant file reads within the same instance lifecycle (unless forced by methods like `deleteTransaction` or `changeInformation`).
* **Record Parsing (`parseRecord`)**:
  * Converts a `CSVRecord` object (from Apache Commons CSV) into a `Transaction` object.
  * Specifically handles the "金额(元)" (Amount) field by removing the leading "¥" symbol before parsing it as a `double`.
* **Adding Transactions (`addTransaction(String filePath, Transaction newTransaction)`)**:
  * **Appends** a new transaction record to the *end* of the specified CSV file.
  * Creates the file if it doesn't exist.
  * Writes the header row only if the file is new or was empty.
  * Formats the payment amount by prepending "¥".
* **Deleting Transactions (`deleteTransaction(String filePath, String orderNumber)`)**:
  * Loads data from the file if not already cached.
  * Filters the cached list to remove the transaction matching the given `orderNumber`.
  * **Overwrites** the *entire* original CSV file with the filtered list using `writeTransactionsToCSV`.
  * Returns `true` if a transaction was removed, `false` otherwise.
* **Updating Transactions (`changeInformation`)**:
  * Loads data if not already cached.
  * Finds the transaction by `orderNumber`.
  * Modifies the specified field (`head`) of the found transaction object in memory.
  * **Crucially, it attempts the update by first *deleting* the original record (using `deleteTransaction`, which overwrites the file) and then *adding* the modified record (using `addTransaction`, which appends to the file).** This logic is inefficient and potentially problematic due to the conflicting file write strategies (overwrite vs. append).
  * Error handling for invalid `head` parameters prints to `System.err`. The method often returns `true` even if the underlying delete/add fails.
* **CSV Writing (`writeTransactionsToCSV`)**:
  * Helper method used by `deleteTransaction`.
  * Writes a given `List<Transaction>` to the specified file path, **overwriting** any existing content.
  * Writes a standard header row.
  * Formats the payment amount by prepending "¥".

#### Important Issues & Considerations

1.  **Incomplete `TransactionDao` Implementation**: The methods declared in the `TransactionDao` interface (`getAllTransactions`, `addTransaction(Transaction)`, `deleteTransaction(String)`, `updateTransaction`, `getTransactionByOrderNumber`) are present but contain only placeholder/stub code. The actual working logic uses separate methods that require the `filePath` to be passed explicitly each time. **This class, in its current state, does not correctly function as a `TransactionDao` implementation.**
2.  **Inconsistent File Writing Strategy**: `addTransaction` *appends* to the file, while `deleteTransaction` and `writeTransactionsToCSV` (used by `deleteTransaction` and `changeInformation`) *overwrite* the entire file. This inconsistency can lead to unexpected behavior, especially within the `changeInformation` method.
3.  **Inefficient Updates/Deletes**: Rewriting the entire CSV file for every single deletion or modification (`changeInformation`) is highly inefficient, especially for large transaction files.
4.  **Flawed Update Logic (`changeInformation`)**: The delete-then-add approach for updates is problematic due to the inconsistent write strategies and potential race conditions or partial failures. The modified record will likely appear at the *end* of the file, not its original position.
5.  **Basic Caching**: The caching mechanism (`isLoad` flag) is simple and does not account for external modifications to the CSV file during the application's runtime.
6.  **Hardcoded Dependencies**: The code relies on specific header names (in Chinese) and the "¥" currency symbol format within the CSV data. Changes to the CSV format would require code modifications.

#### Usage Example (Using Functional Methods)

```java
import DAO.CsvTransactionDao; // Assuming TransactionDao interface exists but is not used directly here due to stub methods
import model.Transaction;
import java.io.IOException;
import java.util.List;

public class CsvDataManager {

    public static void main(String[] args) {
        // NOTE: Using the class directly, not via the TransactionDao interface
        // because the interface methods are not fully implemented.
        CsvTransactionDao csvDao = new CsvTransactionDao();
        String csvFilePath = "path/to/your/transactions.csv"; // <--- Specify your file path

        try {
            // --- Load Data ---
            System.out.println("Loading transactions...");
            List<Transaction> transactions = csvDao.loadFromCSV(csvFilePath);
            System.out.println("Loaded " + transactions.size() + " transactions.");
            // Display first transaction if available
            if (!transactions.isEmpty()) {
                 System.out.println("First transaction order number: " + transactions.get(0).getOrderNumber());
            }

            // --- Add a New Transaction (Appends) ---
            Transaction newTx = new Transaction(
                 "2025-04-20 08:14:00", "Test Add", "Counterparty", "Item", "支",
                 10.0, "支付宝", "交易成功", "NEW_ORDER_123", "MERCH_456", "Test via DAO add"
            );
            System.out.println("\nAdding new transaction (append): " + newTx.getOrderNumber());
            // csvDao.addTransaction(csvFilePath, newTx); // Uncomment to actually add
            System.out.println("Transaction potentially added.");

            // --- Delete a Transaction (Overwrites File) ---
            String orderToDelete = "SOME_EXISTING_ORDER_NUMBER"; // <--- Change to a real order number from your file
            System.out.println("\nAttempting to delete transaction: " + orderToDelete);
            // boolean deleted = csvDao.deleteTransaction(csvFilePath, orderToDelete); // Uncomment to actually delete
            // if (deleted) {
            //     System.out.println("Transaction deleted successfully (File rewritten).");
            // } else {
            //     System.out.println("Transaction not found for deletion.");
            // }

            // --- Change Information (Overwrites then Appends - Use with Caution) ---
             String orderToChange = "ANOTHER_EXISTING_ORDER_NUMBER"; // <--- Change to a real order number
             String fieldToChange = "remarks";
             String newValue = "Updated remark via changeInformation";
             System.out.println("\nAttempting to change info for: " + orderToChange);
             // boolean changed = csvDao.changeInformation(orderToChange, fieldToChange, newValue, csvFilePath); // Uncomment with caution
             // System.out.println("Change operation attempted (File rewritten and appended). Result: " + changed);


        } catch (IOException e) {
            System.err.println("Error interacting with CSV file: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing amount in CSV: " + e.getMessage());
        }
    }
}
```

## Controller (Swing GUI)

Responsibilities: Manage user interface components (buttons, tables, text boxes) to listen for 
events (clicks, inputs) and call service layer methods to update the interface data display

## attention
encoding with UTF-8
