package Service.AIservice;
// Remove import Service.Impl.TransactionServiceImpl;
// Remove import Service.TransactionService;
// Remove import Constants.ConfigConstants; // ConfigConstants might still be needed for API key logic if not elsewhere
// Remove import DAO.CsvTransactionDao; // No longer directly used
// Remove import Utils.CacheUtil; // No longer directly used

import DAO.TransactionDao; // Use the interface
import DAO.Impl.CsvTransactionDao; // Use the implementation to create instance for loader
import Utils.CacheManager; // Import CacheManager
import model.Transaction; // Import Transaction

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

import static Constants.CaffeineKeys.TRANSACTION_CAFFEINE_KEY;
// Remove import static Constants.ConfigConstants.CSV_PATH; // No longer use static CSV_PATH

public class AITransactionService {
    // Keep static ArkService as it's typically thread-safe and stateless
    private static final String API_KEY = System.getenv("ARK_API_KEY"); // Or load from config.properties
    private static final ArkService service = ArkService.builder()
            .timeout(Duration.ofSeconds(1800))
            .connectTimeout(Duration.ofSeconds(20))
            .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .apiKey(API_KEY) // Ensure API_KEY is loaded
            .build();

    // Remove direct DAO instance
    // private final CsvTransactionDao transactionDao = new CsvTransactionDao();

    // Remove direct CacheUtil instance
    // private final CacheUtil<String, List<Transaction>, Exception> cache;

    /**
     * Constructor no longer initializes DAO or Cache directly.
     * DAO and Cache are accessed via CacheManager.
     */
    public AITransactionService() {
        // No initialization needed here related to DAO or Cache
        // The DAO and CacheManager are used in the methods that need data access.
        // System.out.println("AITransactionService initialized.");
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
            if (transactionDetails.isEmpty() || (transactionDetails.size() == 1 && transactionDetails.get(0).startsWith("该时间段内没有交易记录"))) {
                return "在该时间段内没有找到符合条件的交易记录，无法进行分析。请检查时间和交易数据。";
            }

            String aiPrompt = userRequest + "\n" + "以下是我的账单信息：\n" + String.join("\n", transactionDetails);
            System.out.println("AI Service: Sending prompt to AI. Prompt length: " + aiPrompt.length());
            return askAi(aiPrompt);
        } catch (IllegalArgumentException e) {
            System.err.println("AI analysis failed due to invalid time format: " + e.getMessage());
            return "AI分析失败: 时间格式不正确。" + e.getMessage();
        }
        catch (Exception e) {
            System.err.println("AI analysis failed during data retrieval or AI call for file: " + filePath);
            e.printStackTrace();
            return "AI分析失败: 获取数据或调用AI服务时发生错误。" + e.getMessage();
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
            throw new IllegalArgumentException("起始时间格式不正确: " + startTimeStr);
        }
        // If endTime parsing fails, treat it as current time as per original logic if endTimeStr was not empty
        if ((endTimeStr != null && !endTimeStr.trim().isEmpty()) && endTime == null) {
            throw new IllegalArgumentException("结束时间格式不正确: " + endTimeStr);
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
            if (t.getInOut().equals("支出") || t.getInOut().equals("支")) { // Normalize "支" to "支出" internally if needed, but compare against source
                amount = -amount;
            } else if (!t.getInOut().equals("收入") && !t.getInOut().equals("收")) {
                System.err.println("Warning: Unknown 收/支 type for transaction: " + t.getOrderNumber() + " - " + t.getInOut());
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
                    String inOut = net >= 0 ? "总收入" : "总支出"; // Changed label to reflect aggregate
                    if (Math.abs(net) < 0.01 && count > 0) { // If net is near zero but there were transactions
                        inOut = "净零"; // Or specify "收支相抵"
                    }
                    return String.format("交易对方: %s, 净%s: %.2f元，交易次数: %d",
                            cp, inOut, Math.abs(net), count);
                })
                .collect(Collectors.toList());
        System.out.println("Formatted grouped results.");


        // Add time range information to the results list
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String rangeInfo = String.format("分析交易时间范围：%s - %s",
                formatter.format(startTime), formatter.format(endTime));
        results.add(0, rangeInfo); // Add range info at the beginning

        return results.isEmpty() ? List.of(rangeInfo, "该时间段内没有交易记录。") : results; // Ensure range info is always included
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
                return "AI服务配置错误: ARK_API_KEY 未设置。";
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
            return "AI请求失败: " + e.getMessage();
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

    // Helper methods like parseDateTime (kept above)
}