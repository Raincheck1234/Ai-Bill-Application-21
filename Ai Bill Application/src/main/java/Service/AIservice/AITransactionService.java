package Service.AIservice;

import DAO.TransactionDao;
import DAO.Impl.CsvTransactionDao;
import Service.TransactionService;
import Utils.CacheManager;
import model.MonthlySummary;
import model.Transaction;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import model.MonthlySummary;

import static Constants.CaffeineKeys.TRANSACTION_CAFFEINE_KEY;

public class AITransactionService {
    // Keep static ArkService as it's typically thread-safe and stateless
    private static final String API_KEY = System.getenv("ARK_API_KEY"); // Or load from config.properties
    private static final ArkService service = ArkService.builder()
            .timeout(Duration.ofSeconds(1800))
            .connectTimeout(Duration.ofSeconds(20))
            .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .apiKey(API_KEY) // Ensure API_KEY is loaded
            .build();

    // Need access to TransactionService to get monthly summaries
    private final TransactionService transactionService; // Inject TransactionService


    /**
     * Constructor now accepts TransactionService instance.
     */
    public AITransactionService(TransactionService transactionService) {
        this.transactionService = transactionService; // Inject the service
        System.out.println("AITransactionService initialized with TransactionService.");
    }

    /**
     * Analyzes transactions from a specific user's file based on user request and time range.
     *
     * @param userRequest The user's natural language request.
     * @param filePath The path to the user's transaction CSV file.
     * @param startTimeStr The start time string for filtering.
     * @param endTimeStr The end time string for filtering.
     * @return AI analysis result as a String.
     */
    public String analyzeTransactions(String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        try {
            // Get transactions for the specified file path using CacheManager
            // Need to pass a DAO instance for the CacheManager's loader if it needs to load from file.
            TransactionDao transactionDaoForLoading = new CsvTransactionDao(); // Create a DAO instance for loading
            List<Transaction> transactions = CacheManager.getTransactions(filePath, transactionDaoForLoading);
            System.out.println("AI Service: Retrieved " + transactions.size() + " transactions for file: " + filePath);


            // Format filtered transactions for the AI prompt
            List<String> transactionDetails = formatTransactions(transactions, startTimeStr, endTimeStr);
            System.out.println("AI Service: Formatted " + transactionDetails.size() + " transactions for AI.");


            // Check if any transactions were found after filtering
            if (transactionDetails.isEmpty() || (transactionDetails.size() == 1 && transactionDetails.get(0).startsWith("No transactions found within this time period"))) {
                return "No transaction records found matching the criteria within this time period, analysis cannot be performed. Please check the time and transaction data.";
            }

            String aiPrompt = userRequest + "\n" + "Here is my billing information:\n" + String.join("\n", transactionDetails);
            System.out.println("AI Service: Sending prompt to AI. Prompt length: " + aiPrompt.length());
            return askAi(aiPrompt);
        } catch (IllegalArgumentException e) {
            System.err.println("AI analysis failed due to invalid time format: " + e.getMessage());
            return "AI analysis failed: Incorrect time format. " + e.getMessage();
        }
        catch (Exception e) {
            System.err.println("AI analysis failed during data retrieval or AI call for file: " + filePath);
            e.printStackTrace();
            return "AI analysis failed: An error occurred while fetching data or calling the AI service. " + e.getMessage();
        }
    }


    // Keep formatTransactions, parseDateTime, askAi methods. Ensure parseDateTime is robust.
    // The formatTransactions method relies on parseDateTime, ensure consistency with TransactionServiceImpl's parser.

    public List<String> formatTransactions(List<Transaction> transactions, String startTimeStr, String endTimeStr) {
        LocalDateTime startTime = parseDateTime(startTimeStr);
        // If end time is empty, use current time
        LocalDateTime endTime = (endTimeStr == null || endTimeStr.trim().isEmpty())
                ? LocalDateTime.now()
                : parseDateTime(endTimeStr);

        if (startTime == null) {
            // Handle the case where start time is invalid.
            // Depending on requirements, you might throw an exception or return an error message list.
            // Throwing IllegalArgumentException is better for analyzeTransactions to catch.
            throw new IllegalArgumentException("Incorrect start time format: " + startTimeStr);
        }
        // If endTime parsing fails, treat it as current time as per original logic if endTimeStr was not empty
        if ((endTimeStr != null && !endTimeStr.trim().isEmpty()) && endTime == null) {
            throw new IllegalArgumentException("Incorrect end time format: " + endTimeStr);
        }
        // If endTimeStr was empty, endTime is already LocalDateTime.now() which is not null.

        System.out.println("Filtering transactions from " + startTime + " to " + endTime);


        List<Transaction> filtered = transactions.stream()
                .filter(t -> {
                    LocalDateTime tTime = parseDateTime(t.getTransactionTime());
                    // Include transactions exactly at startTime, exclude transactions exactly at endTime (standard range behavior [start, end))
                    // If endTime should be inclusive, change isBefore(startTime) to !isAfter(startTime) and isAfter(endTime) to !isBefore(endTime)
                    // Or use isBefore(startTime) || isAfter(endTime) and negate.
                    // Let's use !isBefore(startTime) && !isAfter(endTime) as it seems more intuitive for a date range, inclusive.
                    return tTime != null && !tTime.isBefore(startTime) && !tTime.isAfter(endTime); // Range [startTime, endTime]
                })
                .collect(Collectors.toList());
        System.out.println("Filtered down to " + filtered.size() + " transactions within range.");


        // Group by Counterparty and summarize net amount and count
        Map<String, double[]> grouped = new HashMap<>(); // double[0] = net amount, double[1] = count
        for (Transaction t : filtered) {
            String counterparty = t.getCounterparty();
            double amount = t.getPaymentAmount();
            // Assuming t.getInOut() returns "Income"/"Expense" or "In"/"Out"
            if (t.getInOut().equalsIgnoreCase("Expense") || t.getInOut().equalsIgnoreCase("Out")) {
                amount = -amount;
            } else if (!t.getInOut().equalsIgnoreCase("Income") && !t.getInOut().equalsIgnoreCase("In")) {
                System.err.println("Warning: Unknown In/Out type for transaction: " + t.getOrderNumber() + " - " + t.getInOut());
                // Decide how to handle unknown types - ignore from analysis? Treat as 0?
                continue; // Skip unknown types for aggregation
            }

            grouped.putIfAbsent(counterparty, new double[]{0.0, 0});
            grouped.get(counterparty)[0] += amount;
            grouped.get(counterparty)[1] += 1;
        }
        System.out.println("Grouped transactions by counterparty. Found " + grouped.size() + " counterparties.");


        List<String> results = grouped.entrySet().stream()
                .map(e -> {
                    String cp = e.getKey();
                    double net = e.getValue()[0];
                    int count = (int) e.getValue()[1];
                    String inOutLabel = net >= 0 ? "Total Income" : "Total Expense";
                    if (Math.abs(net) < 0.01 && count > 0) { // If net is near zero but there were transactions
                        inOutLabel = "Net Zero"; // Or specify "Income equals Expense"
                    }
                    return String.format("Counterparty: %s, Net %s: %.2f CNY, Transaction Count: %d",
                            cp, inOutLabel, Math.abs(net), count);
                })
                .collect(Collectors.toList());
        System.out.println("Formatted grouped results.");


        // Add time range information to the results list
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String rangeInfo = String.format("Analysis Time Range: %s - %s",
                formatter.format(startTime), formatter.format(endTime));
        results.add(0, rangeInfo); // Add range info at the beginning

        if (filtered.isEmpty()) { // Check if the filtered list was empty before grouping
            return List.of(rangeInfo, "No transactions found within this time period.");
        }
        return results;
    }


    // Keep parseDateTime method - ensure it matches the one in TransactionServiceImpl
    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // Clean whitespace
        timeStr = timeStr.trim().replaceAll("\\s+", " ");

        // Append time if only date
        if (timeStr.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            timeStr += " 00:00"; // Assuming minutes format
        } else if (timeStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            timeStr += " 00:00:00"; // Assuming seconds format
        }


        // Try parsing with multiple formats
        List<String> patterns = List.of(
                "yyyy/M/d H:mm", "yyyy/M/d HH:mm",
                "yyyy/MM/d H:mm", "yyyy/MM/d HH:mm",
                "yyyy/M/dd H:mm", "yyyy/M/dd HH:mm",
                "yyyy/MM/dd H:mm", "yyyy/MM/dd HH:mm",
                "yyyy/MM/dd HH:mm:ss", // Added seconds format
                "yyyy-MM-dd HH:mm:ss", // Added dash format
                "yyyy/MM/dd" // Date only (handled above)
                // Add more patterns if needed based on your CSV data
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception ignored) {
                // Ignore parsing errors for this pattern and try the next
            }
        }
        System.err.println("AI Service: Failed to parse date string: " + timeStr);
        return null; // Return null if no pattern matches
    }


    // Keep askAi method
    public String askAi(String prompt) {
        try {
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                System.err.println("ARK_API_KEY environment variable is not set.");
                return "AI service configuration error: ARK_API_KEY not set.";
            }
            // Ensure the static service instance is properly built with the key
            // This might be better done once at application startup if API_KEY is loaded from config.
            // For now, relying on the static final initialization is acceptable if the env var is set before class loading.


            List<ChatMessage> messages = List.of(
                    ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build()
            );

            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model("ep-20250308174053-7pbkq") // Use your model name
                    .messages(messages)
                    .build();

            System.out.println("AI Service: Sending request to VolcEngine Ark...");
            // Use the static service instance
            String responseContent = (String) service.createChatCompletion(chatCompletionRequest)
                    .getChoices().get(0).getMessage().getContent();
            System.out.println("AI Service: Received response from AI.");
            return responseContent;

        } catch (Exception e) {
            System.err.println("AI Service: AI request failed.");
            e.printStackTrace();
            return "AI request failed: " + e.getMessage();
        }
    }

    // Keep runAiInThread method, ensure it uses the correct analyzeTransactions method
    public void runAiInThread(String userRequest, String filePath,String startTimeStr, String endTimeStr) {
        // ExecutorService should ideally be managed at a higher level in a larger app,
        // but a simple single thread executor per request is acceptable for this scale.
        // However, this creates a new thread and executor every time.
        // A fixed thread pool managed statically or by a dedicated AI Service Manager would be more efficient.
        // For now, let's keep it simple as in the original code.

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            // Call the instance method analyzeTransactions
            String result = this.analyzeTransactions(userRequest, filePath,startTimeStr, endTimeStr);
            System.out.println("AI analysis thread finished. Result: " + result);
            // TODO: How to pass the result back to the UI?
            // This thread doesn't have access to the UI components directly.
            // Need a mechanism like a callback or SwingUtilities.invokeLater.
            // This will be addressed when integrating AI output into the UI (Step 10).
        });
        // Consider shutting down the executor more gracefully, e.g., when the app exits.
        // executor.shutdown(); // Shutting down immediately might cancel the task
        // A better approach is `executor.shutdown()` after submitting, but manage the executor lifecycle elsewhere.
    }

    /**
     * Generates a personal consumption summary based on monthly data.
     * @param userFilePath The path to the user's transaction CSV file. (Might not be strictly needed if service handles context)
     * @return AI analysis result as a String.
     */
    public String generatePersonalSummary(String userFilePath) {
        try {
            // Get monthly summary data from TransactionService
            // Note: TransactionService already operates on the current user's data implicitly if passed correctly.
            // We might not need userFilePath explicitly in this method signature if the service instance is user-specific.
            // Let's assume the injected transactionService is already scoped to the current user.
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("AI Service: Retrieved " + summaries.size() + " months of summary data.");

            if (summaries.isEmpty()) {
                return "Not enough transaction data found to generate a personal spending summary.";
            }

            // Format the summary data for the AI prompt
            StringBuilder summaryPromptBuilder = new StringBuilder();
            summaryPromptBuilder.append("Please generate a personal spending habits summary based on the following monthly data. Analyze main expense categories, monthly trends, and assess my spending health:\n\n");

            // Sort months chronologically for better trend analysis by AI
            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                summaryPromptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                summaryPromptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                summaryPromptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                summaryPromptBuilder.append("  Expense Breakdown:\n");
                if (ms.getExpenseByCategory().isEmpty()) {
                    summaryPromptBuilder.append("    (No expenses)\n");
                } else {
                    // Sort categories by amount descending for AI to easily see major categories
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    summaryPromptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                            );
                }
                summaryPromptBuilder.append("\n"); // Add space between months
            }

            String aiPrompt = summaryPromptBuilder.toString();
            System.out.println("AI Service: Sending personal summary prompt to AI. Prompt length: " + aiPrompt.length());

            return askAi(aiPrompt); // Call the generic AI method
        } catch (Exception e) {
            System.err.println("AI Service: Failed to generate personal summary.");
            e.printStackTrace();
            return "Failed to generate personal spending summary: " + e.getMessage();
        }
    }

    /**
     * Generates suggestions for savings goals based on monthly data.
     * @param userFilePath The path to the user's transaction CSV file. (Might not be strictly needed)
     * @return AI suggestions as a String.
     */
    public String suggestSavingsGoals(String userFilePath) {
        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("AI Service: Retrieved " + summaries.size() + " months of summary data for savings goal suggestion.");

            if (summaries.isEmpty()) {
                return "Not enough transaction data found to suggest savings goals.";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Please provide some reasonable savings goal suggestions for my spending habits based on the following monthly income and expense summary data:\n\n");

            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                double net = ms.getTotalIncome() - ms.getTotalExpense();
                promptBuilder.append("  Monthly Net Income/Expense: ").append(String.format("%.2f", net)).append(" CNY\n");
                promptBuilder.append("\n");
            }

            String aiPrompt = promptBuilder.toString();
            System.out.println("AI Service: Sending savings goals prompt to AI. Prompt length: " + aiPrompt.length());

            return askAi(aiPrompt);
        } catch (Exception e) {
            System.err.println("AI Service: Failed to suggest savings goals.");
            e.printStackTrace();
            return "Failed to suggest savings goals: " + e.getMessage();
        }
    }

    /**
     * Generates personalized cost-cutting recommendations based on monthly data.
     * @param userFilePath The path to the user's transaction CSV file. (Might not be strictly needed)
     * @return AI recommendations as a String.
     */
    public String givePersonalSavingTips(String userFilePath) {
        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("AI Service: Retrieved " + summaries.size() + " months of summary data for saving tips.");

            if (summaries.isEmpty()) {
                return "Not enough transaction data found to provide personalized saving tips.";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Please provide some targeted cost-saving suggestions for me based on the following monthly spending summary data:\n\n");

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
                    // Sort categories by amount descending
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }

            String aiPrompt = promptBuilder.toString();
            System.out.println("AI Service: Sending personal saving tips prompt to AI. Prompt length: " + aiPrompt.length());

            return askAi(aiPrompt);
        } catch (Exception e) {
            System.err.println("AI Service: Failed to give personal saving tips.");
            e.printStackTrace();
            return "Failed to generate personalized saving tips: " + e.getMessage();
        }
    }

    public String analyzeSeasonalSpendingPatterns(String userFilePath) {
        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            System.out.println("AI Service: Retrieved " + summaries.size() + " months of summary data for detailed seasonal analysis.");

            if (summaries.isEmpty()) {
                return "Not enough monthly transaction data found to analyze detailed seasonal spending patterns.";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("I am a user in China. Please analyze my monthly financial data below to identify seasonal spending patterns and provide budgeting advice. Focus on the following aspects:\n\n");

            // 1. 法定节假日支出分析和预算建议
            promptBuilder.append("1.  **Public Holiday Spending Analysis & Budgeting Advice:**\n");
            promptBuilder.append("    *   Analyze spending around major Chinese public holidays: Spring Festival (Chinese New Year, typically Jan/Feb), Qingming Festival (April), Labor Day (May 1st), Dragon Boat Festival (Duanwu, typically May/June), National Day (Oct 1st), and New Year's Day (Jan 1st).\n");
            promptBuilder.append("    *   Identify any significant increases or changes in spending categories (e.g., travel, gifts, dining out, red packets/hongbao) during these holiday periods.\n");
            promptBuilder.append("    *   Provide specific budgeting suggestions to prepare for these holidays. For example, how much should I consider setting aside in the months leading up to these holidays based on my past spending?\n\n");

            // 2. 季节变化与衣物支出，以及季节性行为不符之处
            promptBuilder.append("2.  **Seasonal Changes & Clothing Expenses:**\n");
            promptBuilder.append("    *   Analyze spending on clothing. Are there noticeable increases during season changes (e.g., spring/summer, autumn/winter transitions)?\n");
            promptBuilder.append("    *   Suggest how much I should budget for seasonal clothing changes.\n");
            promptBuilder.append("    *   Identify any spending patterns that seem unusual for the season in China (e.g., high spending on winter clothing in summer, or vice-versa). If such inconsistencies are found, please point them out.\n\n");

            promptBuilder.append("Please provide clear, actionable insights and advice based on the data. Here is my monthly financial data:\n\n");


            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  Total Income: ").append(String.format("%.2f", ms.getTotalIncome())).append(" CNY\n");
                promptBuilder.append("  Total Expense: ").append(String.format("%.2f", ms.getTotalExpense())).append(" CNY\n");
                promptBuilder.append("  Net (Income - Expense): ").append(String.format("%.2f", ms.getTotalIncome() - ms.getTotalExpense())).append(" CNY\n");
                promptBuilder.append("  Expense Breakdown:\n");
                if (ms.getExpenseByCategory().isEmpty()) {
                    promptBuilder.append("    (No expenses recorded this month)\n");
                } else {
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f CNY\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }

            String aiPrompt = promptBuilder.toString();
            System.out.println("AI Service: Sending detailed seasonal spending analysis prompt to AI. Prompt length: " + aiPrompt.length());

            return askAi(aiPrompt);

        } catch (Exception e) {
            System.err.println("AI Service: Failed to analyze detailed seasonal spending patterns.");
            e.printStackTrace();
            return "Failed to analyze detailed seasonal spending patterns: " + e.getMessage();
        }
    }



    // ... Keep other methods like analyzeTransactions, formatTransactions, parseDateTime, askAi ...

    // The existing CollegeStudentNeeds class also has budget and tips methods.
    // We need to decide: should AITransactionService offer general AI for anyone,
    // and CollegeStudentNeeds offer student-specific prompts/logic?
    // Or should AITransactionService be the main AI interaction point,
    // and CollegeStudentNeeds just holds student-specific logic/prompts used by AITransactionService?
    // Given the project structure, it might be better to keep student logic in CollegeStudentNeeds
    // and call it from MenuUI or a wrapper service.
    // Let's adjust: generatePersonalSummary, suggestSavingsGoals, givePersonalSavingTips will use monthly summary.
    // CollegeStudentNeeds.generateBudget and generateTipsForSaving can remain using their current logic
    // (budget uses weekly expenses, tips is generic for now).
    // The prompt for CollegeStudentNeeds.generateBudget might need to be updated to use the monthly summary data too for better context.
    // Let's refine CollegeStudentNeeds methods in the next step.

    // For now, the three new methods above will use the monthly summary.
    // The existing analyzeTransactions method in AITransactionService and the methods in CollegeStudentNeeds remain as is for now,
    // but their usage in UI might change slightly.

}