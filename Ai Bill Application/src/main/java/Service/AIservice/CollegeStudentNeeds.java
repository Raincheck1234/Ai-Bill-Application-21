package Service.AIservice;

import Constants.CaffeineKeys; // Import CaffeineKeys if using CacheManager internally (which it is)
import Constants.ConfigConstants;
import DAO.TransactionDao; // Use the interface
import DAO.Impl.CsvTransactionDao; // Use the implementation for CacheManager loading
// Remove import Service.Impl.TransactionServiceImpl;
import Utils.CacheManager; // Import CacheManager
import model.Transaction;

import java.io.IOException;
import java.time.LocalDate; // Using LocalDate for date calculations
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // For parsing errors
import java.time.temporal.ChronoUnit; // For calculating days between dates
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit; // Might be used by CacheUtil if it weren't static
import java.util.stream.Collectors;

// Remove DAO and CacheUtil fields
// CsvTransactionDao dao;
// private final CacheUtil<String, List<Transaction>, Exception> cache;


public class CollegeStudentNeeds {
    private final String requestBudge="我是一名预算有限的大学生，请根据下面我给出的花费，帮助我给下周预算范围，必须以[最低预算，最高预算],的方式给出回答，不能有多余的回复。";
    private final String requestTips="我是一名预算有限的大学生，请给我推荐一些省钱方法。";
    private final String requestRecognition="下面我将给你一些账单的信息，请推测这个账单是什么方面的消费: ";

    // AITransactionService is used for asking AI, can be an instance or created on demand
    private final AITransactionService aiService = new AITransactionService(); // Create an instance

    /**
     * Constructor no longer initializes DAO or Cache directly.
     */
    public CollegeStudentNeeds() {
        // No initialization needed here related to DAO or Cache
        // Data access is done via CacheManager in the methods that need it.
        // System.out.println("CollegeStudentNeeds initialized.");
    }

    /**
     * Recognizes the spending category of a single transaction using AI.
     *
     * @param transaction The transaction to recognize.
     * @return The AI's suggested category.
     */
    public String RecognizeTransaction(Transaction transaction){
        if (transaction == null) {
            return "无法识别空交易信息";
        }
        StringBuilder sb=new StringBuilder();
        // Provide relevant transaction details to the AI
        sb.append("交易类型:").append(transaction.getTransactionType()).append(",")
                .append("交易对方:").append(transaction.getCounterparty()).append(",")
                .append("商品:").append(transaction.getCommodity()).append(",")
                .append("收/支:").append(transaction.getInOut()).append(",")
                .append("金额(元):").append(transaction.getPaymentAmount()).append(",") // Use formatted amount if needed, but raw double is fine for AI
                .append("支付方式:").append(transaction.getPaymentMethod()).append(",")
                .append("备注:").append(transaction.getRemarks());

        System.out.println("CollegeStudentNeeds: Sending recognition request to AI: " + sb.toString());
        return  aiService.askAi(requestRecognition + sb.toString());
    }

    /**
     * Generates saving tips for college students using AI.
     * @return AI's suggested saving tips.
     */
    public String generateTipsForSaving(){
        System.out.println("CollegeStudentNeeds: Sending saving tips request to AI.");
        return  aiService.askAi(requestTips);
    }

    /**
     * Analyzes weekly spending from a user's transaction history and asks AI for a budget range.
     *
     * @param filePath The path to the user's transaction CSV file.
     * @return A double array [minBudget, maxBudget] parsed from AI response, or [-1, -1] on failure.
     * @throws IOException If there's an error accessing the transaction data.
     */
    // 按周统计已有的支出，依靠ai得到下周的预算
    public double[] generateBudget(String filePath) throws Exception { // Changed signature to throw Exception
        List<Transaction> transactions;
        try {
            // Get transactions using CacheManager for the specified file path
            TransactionDao transactionDaoForLoading = new CsvTransactionDao(); // Create DAO instance for loading
            transactions = CacheManager.getTransactions(filePath, transactionDaoForLoading);
            System.out.println("CollegeStudentNeeds: Retrieved " + transactions.size() + " transactions for budget analysis from: " + filePath);
        } catch (Exception e) {
            System.err.println("CollegeStudentNeeds: Error retrieving transactions for budget analysis: " + filePath);
            e.printStackTrace();
            throw e; // Re-throw the exception from CacheManager/DAO
        }


        int size = transactions.size();
        if (size == 0) {
            System.out.println("CollegeStudentNeeds: No transactions found for budget analysis.");
            return new double[]{-1, -1}; // Indicate no data
        }

        // Filter for '支出' transactions and sort them by date (newest first)
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getInOut().equals("支出") || t.getInOut().equals("支")) // Filter for expenses
                // Safely parse dates for sorting
                .sorted((t1, t2) -> {
                    LocalDate date1 = parseDateSafe(t1.getTransactionTime().split(" ")[0]);
                    LocalDate date2 = parseDateSafe(t2.getTransactionTime().split(" ")[0]);
                    if (date1 != null && date2 != null) {
                        return date2.compareTo(date1); // Sort by date, newest first
                    } else if (date1 == null && date2 == null) {
                        return 0;
                    } else if (date1 == null) {
                        return 1; // Unparseable dates towards the end
                    } else { // date2 == null
                        return -1;
                    }
                })
                .toList();
        System.out.println("CollegeStudentNeeds: Filtered " + expenseTransactions.size() + " expense transactions.");


        if (expenseTransactions.isEmpty()) {
            System.out.println("CollegeStudentNeeds: No expense transactions found for budget analysis.");
            return new double[]{-1, -1}; // Indicate no expense data
        }

        List<Double> weeklyExpenses = new ArrayList<>();
        LocalDate currentWeekStart = null;
        double currentWeekTotal = 0;

        for (Transaction expense : expenseTransactions) {
            LocalDate transactionDate = parseDateSafe(expense.getTransactionTime().split(" ")[0]);
            if (transactionDate == null) {
                System.err.println("CollegeStudentNeeds: Skipping transaction with unparseable date: " + expense.getTransactionTime());
                continue; // Skip transactions with invalid dates
            }

            if (currentWeekStart == null) {
                // Start the first week with the newest transaction date
                currentWeekStart = transactionDate;
            }

            // Calculate days between current transaction and the start of the current tracking week
            // ChronoUnit.DAYS.between(date1, date2) calculates date2 - date1.
            // We want the difference between transactionDate and currentWeekStart.
            // If transactionDate is currentWeekStart or within 6 days *before* currentWeekStart, it's the same week.
            long daysDifference = ChronoUnit.DAYS.between(transactionDate, currentWeekStart); // How many days tDate is before/at cwStart

            if (daysDifference >= 0 && daysDifference < 7) {
                // Transaction is within the current 7-day period (week) starting from currentWeekStart backwards
                currentWeekTotal += expense.getPaymentAmount();
            } else if (daysDifference >= 7) {
                // Transaction is in a previous week. Store current week's total.
                weeklyExpenses.add(currentWeekTotal);
                // Start a new week with the current transaction's date as the new week's "start" reference point
                currentWeekStart = transactionDate;
                currentWeekTotal = expense.getPaymentAmount(); // Start the new week's total with this expense
            }
            // If daysDifference is negative, it means the transaction date is *after* the currentWeekStart,
            // which shouldn't happen if sorted by date desc. If it does, it indicates a sorting or logic error.
        }

        // Add the total for the last tracked week
        if (currentWeekTotal > 0 || currentWeekStart != null) { // Add the last week if there were expenses or it was started
            weeklyExpenses.add(currentWeekTotal);
        }

        System.out.println("CollegeStudentNeeds: Calculated weekly expenses for " + weeklyExpenses.size() + " weeks: " + weeklyExpenses);

        if (weeklyExpenses.isEmpty()) {
            return new double[]{-1, -1}; // No complete weeks with expenses found
        }

        // Format weekly expenses for the AI prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(requestBudge).append("\n");
        for (int i = 0; i < weeklyExpenses.size(); i++) {
            // Count weeks from newest to oldest for reporting to AI
            promptBuilder.append("第");
            promptBuilder.append(weeklyExpenses.size() - 1 - i); // Count backwards (0 is newest week)
            promptBuilder.append("周:花费");
            promptBuilder.append(String.format("%.2f", weeklyExpenses.get(i))); // Format amount
            promptBuilder.append("元;");
        }

        String aiPrompt = promptBuilder.toString();
        System.out.println("CollegeStudentNeeds: Sending budget request to AI. Prompt: " + aiPrompt);

        String answer = aiService.askAi(aiPrompt);
        System.out.println("CollegeStudentNeeds: Received budget response from AI: " + answer);

        // Parse the budget range from the AI's answer
        double[] ret = parseDoubleArrayFromString(answer);
        if (ret == null || ret.length != 2) {
            System.err.println("CollegeStudentNeeds: Failed to parse budget array from AI response: " + answer);
            return new double[]{-1, -1}; // Indicate parsing failure
        }

        return ret;
    }

    // Helper method to parse date string safely (yyyy/MM/dd)
    private LocalDate parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;

        // Clean whitespace and replace potential hyphens with slashes if the expected format is slash-separated
        dateStr = dateStr.trim().replace('-', '/').replaceAll("\\s+", "");

        // Try parsing with multiple slash formats (consistent with parseDateTime logic)
        List<String> patterns = List.of(
                "yyyy/M/d", "yyyy/MM/d", "yyyy/M/dd", "yyyy/MM/dd"
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Ignore parsing errors for this pattern
            }
        }
        System.err.println("CollegeStudentNeeds: Failed to parse date string for budget analysis: " + dateStr);
        return null; // Return null if no pattern matches
    }


    // Keep parseDoubleArrayFromString method - ensure robustness
    public double[] parseDoubleArrayFromString(String input) {
        if (input == null) {
            return null;
        }
        String trimmedInput = input.trim();
        System.out.println("CollegeStudentNeeds: Attempting to parse budget string: '" + trimmedInput + "'");

        // Look for the first '[' and the last ']'
        int startIndex = trimmedInput.indexOf('[');
        int endIndex = trimmedInput.lastIndexOf(']');

        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) {
            System.err.println("CollegeStudentNeeds: Budget string does not contain valid []. Input: " + trimmedInput);
            return null; // Invalid format
        }

        String content = trimmedInput.substring(startIndex + 1, endIndex).trim(); // Get content between brackets

        // Split by comma, handling potential spaces around comma
        String[] numberStrings = content.split("\\s*,\\s*"); // Split by comma surrounded by optional whitespace

        if (numberStrings.length != 2) {
            System.err.println("CollegeStudentNeeds: Budget string content does not contain exactly two numbers separated by comma. Content: " + content);
            return null; // Expecting exactly two numbers
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
            return null; // Indicate parsing failure
        }
    }

    // The convertDateToNumber method is no longer needed as we are using LocalDate
    // private int convertDateToNumber(String date) { ... }
}