package Service.AIservice;

import Constants.StandardCategories;
import DAO.TransactionDao;
import DAO.Impl.CsvTransactionDao;
// Removed: import Service.Impl.TransactionServiceImpl; // Not directly used here
import Utils.CacheManager;
import model.Transaction;
import model.MonthlySummary;
import Service.TransactionService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class CollegeStudentNeeds {
    // Prompts for AI interaction
    private final String requestBudge = "I am a college student with a limited budget. Based on my historical weekly spending and monthly income/expense summary provided below, please help me set a budget range for next week. You must provide the answer in the format [minimum_budget, maximum_budget], with no additional text.";
    private final String requestTips = "I am a college student with a limited budget. Based on my monthly spending summary data provided below, please recommend some targeted ways for me to save money.";
    private final String requestRecognition =
            "Please infer the most appropriate transaction type based on the following billing information. The returned type must exactly match one of the entries in the following list:\n" +
                    StandardCategories.getAllCategoriesString() + "\n" + // Include the list of valid categories
                    "If it cannot be determined, please return 'Other Expenses' or 'Other Income' (depending on the income/expense direction). Return only the type string, do not include additional text or explanations. Billing information:";

    // TransactionService is injected to access transaction data and summaries.
    private final TransactionService transactionService;

    /**
     * Constructor that accepts a TransactionService instance.
     * @param transactionService The service to interact with transaction data.
     */
    public CollegeStudentNeeds(TransactionService transactionService) {
        this.transactionService = transactionService;
        System.out.println("CollegeStudentNeeds initialized with TransactionService.");
    }

    /**
     * Recognizes the spending category of a single transaction using AI.
     * This method uses the raw transaction details for recognition.
     *
     * @param transaction The transaction to recognize.
     * @return The AI's suggested category.
     */
    public String RecognizeTransaction(Transaction transaction) {
        if (transaction == null) {
            return "Cannot recognize empty transaction information";
        }
        StringBuilder sb = new StringBuilder();
        // Build the string with transaction details for the AI prompt.
        // Assuming In/Out and other fields are in English or will be handled by AI.
        sb.append("Transaction Type:").append(transaction.getTransactionType()).append(",")
                .append("Counterparty:").append(transaction.getCounterparty()).append(",")
                .append("Commodity:").append(transaction.getCommodity()).append(",")
                .append("In/Out:").append(transaction.getInOut()).append(",")
                .append("Amount(CNY):").append(String.format("%.2f", transaction.getPaymentAmount())).append(",")
                .append("Payment Method:").append(transaction.getPaymentMethod()).append(",")
                .append("Remarks:").append(transaction.getRemarks());

        System.out.println("CollegeStudentNeeds: Sending recognition request to AI: " + sb.toString());
        // Create a local AITransactionService instance for askAi calls, as it doesn't need an injected TransactionService for this specific task.
        AITransactionService localAiService = new AITransactionService(null);
        return localAiService.askAi(requestRecognition + sb.toString());
    }

    /**
     * Generates saving tips for college students using AI, based on their monthly summary.
     * @param userFilePath The path to the user's transaction CSV file. This parameter might be
     *                     refactored if TransactionService is inherently user-scoped.
     * @return AI's suggested saving tips.
     */
    public String generateTipsForSaving(String userFilePath) {
        try {
            // Get monthly summary data using the injected TransactionService.
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("CollegeStudentNeeds: Retrieved " + summaries.size() + " months of summary data for tips.");

            if (summaries.isEmpty()) {
                return "Not enough transaction data found to provide personalized saving tips.";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(requestTips).append("\n\nHere is my monthly spending summary data:\n\n");

            // Sort months chronologically to present data in order to the AI.
            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                promptBuilder.append("  Expense Breakdown:\n");
                if (ms.getExpenseByCategory().isEmpty()) {
                    promptBuilder.append("    (No expenses)\n");
                } else {
                    // Sort categories by amount in descending order for clarity.
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }

            String aiPrompt = promptBuilder.toString();
            System.out.println("CollegeStudentNeeds: Sending saving tips prompt to AI. Prompt length: " + aiPrompt.length());

            AITransactionService localAiService = new AITransactionService(null);
            return localAiService.askAi(aiPrompt);

        } catch (Exception e) {
            System.err.println("CollegeStudentNeeds: Failed to generate saving tips.");
            e.printStackTrace();
            return "Failed to generate personalized saving tips: " + e.getMessage();
        }
    }

    /**
     * Analyzes weekly spending and monthly summaries to ask AI for a budget range.
     * @param filePath The path to the user's transaction CSV file.
     * @return A double array [minBudget, maxBudget] parsed from AI response, or [-1, -1] on failure.
     * @throws Exception If there's an error accessing transaction data or summaries.
     */
    public double[] generateBudget(String filePath) throws Exception {
        List<Transaction> transactions;
        Map<String, MonthlySummary> summaries;

        try {
            // Get transactions using CacheManager.
            TransactionDao transactionDaoForLoading = new CsvTransactionDao();
            transactions = CacheManager.getTransactions(filePath, transactionDaoForLoading);
            System.out.println("CollegeStudentNeeds: Retrieved " + transactions.size() + " transactions for budget analysis from: " + filePath);

            // Get monthly summary data for additional context.
            summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("CollegeStudentNeeds: Retrieved " + summaries.size() + " months of summary data for budget context.");

        } catch (Exception e) {
            System.err.println("CollegeStudentNeeds: Error retrieving transactions or summary for budget analysis: " + filePath);
            e.printStackTrace();
            throw e;
        }

        // Handle case with no transactions.
        if (transactions.isEmpty()) {
            System.out.println("CollegeStudentNeeds: No transactions found for budget analysis.");
            if (!summaries.isEmpty()) {
                // If monthly summaries exist, use them for the AI prompt.
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Here is my monthly income and expense summary data:\n\n");
                List<String> sortedMonths = new ArrayList<>(summaries.keySet());
                Collections.sort(sortedMonths);
                for (String month : sortedMonths) {
                    MonthlySummary ms = summaries.get(month);
                    promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                    promptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                    promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                    double net = ms.getTotalIncome() - ms.getTotalExpense();
                    promptBuilder.append("  Monthly Net Income/Expense: ").append(String.format("%.2f", net)).append(" CNY\n");
                    promptBuilder.append("  Main Expense Categories:\n");
                    if (ms.getExpenseByCategory().isEmpty()) {
                        promptBuilder.append("    (No expenses)\n");
                    } else {
                        ms.getExpenseByCategory().entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                .forEach(entry ->
                                        promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                                );
                    }
                    promptBuilder.append("\n");
                }
                String answer = new AITransactionService(null).askAi(requestBudge + "\n\nNo weekly spending data found.\n" + promptBuilder.toString());
                return parseDoubleArrayFromString(answer);
            }
            return new double[]{-1, -1}; // No data at all.
        }

        // Filter for 'Expense' transactions and sort them by date (newest first).
        // Assumes In/Out field uses "Expense" or "Out" for expense transactions.
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getInOut() != null && (t.getInOut().equalsIgnoreCase("Expense") || t.getInOut().equalsIgnoreCase("Out")))
                .sorted((t1, t2) -> {
                    LocalDate date1 = parseDateSafe(t1.getTransactionTime());
                    LocalDate date2 = parseDateSafe(t2.getTransactionTime());
                    if (date1 != null && date2 != null) { return date2.compareTo(date1); }
                    else if (date1 == null && date2 == null) { return 0; }
                    else if (date1 == null) { return 1; } // Treat null dates as later for sorting purposes if needed.
                    else { return -1; }
                })
                .collect(Collectors.toList());
        System.out.println("CollegeStudentNeeds: Filtered " + expenseTransactions.size() + " expense transactions for budget analysis.");

        // Handle case with no expense transactions.
        if (expenseTransactions.isEmpty()) {
            System.out.println("CollegeStudentNeeds: No expense transactions found for budget analysis.");
            if (!summaries.isEmpty()) {
                // If monthly summaries exist, use them for the AI prompt.
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Here is my monthly income and expense summary data:\n\n");
                List<String> sortedMonths = new ArrayList<>(summaries.keySet());
                Collections.sort(sortedMonths);
                for (String month : sortedMonths) {
                    MonthlySummary ms = summaries.get(month);
                    promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                    promptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                    promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                    double net = ms.getTotalIncome() - ms.getTotalExpense();
                    promptBuilder.append("  Monthly Net Income/Expense: ").append(String.format("%.2f", net)).append(" CNY\n");
                    promptBuilder.append("  Main Expense Categories:\n");
                    if (ms.getExpenseByCategory().isEmpty()) {
                        promptBuilder.append("    (No expenses)\n");
                    } else {
                        ms.getExpenseByCategory().entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                .forEach(entry ->
                                        promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                                );
                    }
                    promptBuilder.append("\n");
                }
                String answer = new AITransactionService(null).askAi(requestBudge + "\n\nNo weekly spending data found.\n" + promptBuilder.toString());
                return parseDoubleArrayFromString(answer);
            }
            return new double[]{-1, -1}; // No expense data and no summary data.
        }

        // Calculate weekly expenses from the filtered expense transactions.
        List<Double> weeklyExpenses = new ArrayList<>();
        LocalDate currentWeekStart = null;
        double currentWeekTotal = 0;

        for (Transaction expense : expenseTransactions) {
            LocalDate transactionDate = parseDateSafe(expense.getTransactionTime());
            if (transactionDate == null) continue;

            if (currentWeekStart == null) {
                currentWeekStart = transactionDate;
            }

            long daysDifference = ChronoUnit.DAYS.between(transactionDate, currentWeekStart);

            if (daysDifference >= 0 && daysDifference < 7) {
                currentWeekTotal += expense.getPaymentAmount();
            } else if (daysDifference >= 7) {
                weeklyExpenses.add(currentWeekTotal);
                currentWeekStart = transactionDate;
                currentWeekTotal = expense.getPaymentAmount();
            }
        }
        if (currentWeekTotal > 0 || currentWeekStart != null) { // Add the last week's total.
            weeklyExpenses.add(currentWeekTotal);
        }
        System.out.println("CollegeStudentNeeds: Calculated weekly expenses for " + weeklyExpenses.size() + " weeks: " + weeklyExpenses);

        // Format the prompt including both weekly expenses and monthly summary.
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(requestBudge).append("\n\n");

        promptBuilder.append("Here is my recent weekly spending data:\n");
        if (weeklyExpenses.isEmpty()) {
            promptBuilder.append("(Not enough periodic expense data found)\n");
        } else {
            for (int i = 0; i < weeklyExpenses.size(); i++) {
                promptBuilder.append("Week ");
                promptBuilder.append(weeklyExpenses.size() - i); // Week numbers count down (e.g., Week 3, Week 2, Week 1 (most recent))
                promptBuilder.append(": Spent ");
                promptBuilder.append(String.format("%.2f", weeklyExpenses.get(i)));
                promptBuilder.append(" CNY; ");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("\nAdditionally, here is my monthly income and expense summary data:\n\n");
        if (summaries.isEmpty()) {
            promptBuilder.append("(No monthly summary data found)\n");
        } else {
            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);
            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                double net = ms.getTotalIncome() - ms.getTotalExpense();
                promptBuilder.append("  Monthly Net Income/Expense: ").append(String.format("%.2f", net)).append(" CNY\n");
                promptBuilder.append("  Main Expense Categories:\n");
                if (ms.getExpenseByCategory().isEmpty()) {
                    promptBuilder.append("    (No expenses)\n");
                } else {
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }
        }

        String aiPrompt = promptBuilder.toString();
        System.out.println("CollegeStudentNeeds: Sending budget request to AI. Prompt length: " + aiPrompt.length());

        String answer = new AITransactionService(null).askAi(aiPrompt);
        System.out.println("CollegeStudentNeeds: Received budget response from AI: " + answer);

        double[] ret = parseDoubleArrayFromString(answer);
        if (ret == null || ret.length != 2) {
            System.err.println("CollegeStudentNeeds: Failed to parse budget array from AI response: " + answer + ". Full AI Response: " + answer);
            return new double[]{-1, -1};
        }
        return ret;
    }

    /**
     * Helper method to safely parse a date string from a transaction's time field.
     * This method attempts to parse the date part of various common timestamp formats.
     * @param timeStr The transaction time string.
     * @return A LocalDate object, or null if parsing fails.
     */
    private LocalDate parseDateSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        String datePart = timeStr.split(" ")[0]; // Get the date part (before space).
        datePart = datePart.trim().replace('-', '/'); // Normalize hyphens to slashes.

        List<String> patterns = List.of(
                "yyyy/M/d", "yyyy/MM/d", "yyyy/M/dd", "yyyy/MM/dd"
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(datePart, formatter);
            } catch (DateTimeParseException ignored) {
                // Ignore parsing errors for this pattern and try the next.
            }
        }
        System.err.println("CollegeStudentNeeds: Failed to parse date part '" + datePart + "' from transaction time: " + timeStr);
        return null;
    }

    /**
     * Parses a string representation of a budget range (e.g., "[100.0, 200.0]") into a double array.
     * @param input The string to parse.
     * @return A double array containing [minBudget, maxBudget], or null if parsing fails.
     */
    public double[] parseDoubleArrayFromString(String input) {
        if (input == null) { return null; }
        String trimmedInput = input.trim();
        System.out.println("CollegeStudentNeeds: Attempting to parse budget string: '" + trimmedInput + "'");
        int startIndex = trimmedInput.indexOf('[');
        int endIndex = trimmedInput.lastIndexOf(']');
        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) {
            System.err.println("CollegeStudentNeeds: Budget string does not contain valid []. Input: " + trimmedInput);
            return null;
        }
        String content = trimmedInput.substring(startIndex + 1, endIndex).trim();
        // Split by comma, allowing for optional spaces around the comma.
        String[] numberStrings = content.split("\\s*,\\s*");
        if (numberStrings.length != 2) {
            System.err.println("CollegeStudentNeeds: Budget string content does not contain exactly two numbers separated by comma. Content: " + content);
            return null;
        }
        double[] result = new double[2];
        try {
            result[0] = Double.parseDouble(numberStrings[0].trim());
            result[1] = Double.parseDouble(numberStrings[1].trim());
            System.out.println("CollegeStudentNeeds: Successfully parsed budget: [" + result[0] + ", " + result[1] + "]");
            return result;
        } catch (NumberFormatException e) {
            System.err.println("CollegeStudentNeeds: Error parsing numbers from budget string: " + content);
            e.printStackTrace();
            return null;
        }
    }
}