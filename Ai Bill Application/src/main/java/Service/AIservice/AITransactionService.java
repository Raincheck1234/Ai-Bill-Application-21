package Service.AIservice;

import DAO.CsvTransactionDao;
import model.Transaction;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

public class AITransactionService {
    private static final String API_KEY = System.getenv("ARK_API_KEY");
    private static final ArkService service = ArkService.builder()
            .timeout(Duration.ofSeconds(1800))
            .connectTimeout(Duration.ofSeconds(20))
            .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .apiKey(API_KEY)
            .build();

    private final CsvTransactionDao transactionDao = new CsvTransactionDao();

    public String analyzeTransactions(String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        try {
            List<Transaction> transactions = transactionDao.loadFromCSV(filePath);
            List<String> transactionDetails = formatTransactions(transactions, startTimeStr, endTimeStr);

            String aiPrompt = userRequest + "\n" + "Here is my transaction information, please analyze within 500 words:\n" + String.join("\n", transactionDetails);
            return askAi(aiPrompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "AI analysis failed: " + e.getMessage();
        }
    }

    public List<String> formatTransactions(List<Transaction> transactions, String startTimeStr, String endTimeStr) {
        LocalDateTime startTime = parseDateTime(startTimeStr);
        LocalDateTime endTime = (endTimeStr == null || endTimeStr.isEmpty())
                ? LocalDateTime.now()
                : parseDateTime(endTimeStr);

        if (startTime == null) {
            throw new IllegalArgumentException("Invalid start time format");
        }

        List<Transaction> filtered = transactions.stream()
                .filter(t -> {
                    LocalDateTime tTime = parseDateTime(t.getTransactionTime());
                    return tTime != null && !tTime.isBefore(startTime) && !tTime.isAfter(endTime);
                })
                .collect(Collectors.toList());

        Map<String, double[]> grouped = new HashMap<>();
        for (Transaction t : filtered) {
            String counterparty = t.getCounterparty();
            double amount = t.getInOut().equals("Expense") ? -t.getPaymentAmount() : t.getPaymentAmount();
            grouped.putIfAbsent(counterparty, new double[]{0.0, 0});
            grouped.get(counterparty)[0] += amount;
            grouped.get(counterparty)[1] += 1;
        }

        List<String> results = grouped.entrySet().stream()
                .map(e -> {
                    String cp = e.getKey();
                    double net = e.getValue()[0];
                    int count = (int) e.getValue()[1];
                    String inOut = net >= 0 ? "Income" : "Expense";
                    return String.format("Counterparty: %s, Type: %s, Amount: %.2f, Transactions: %d",
                            cp, inOut, Math.abs(net), count);
                })
                .collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        results.add(String.format("Transaction period: %s - %s",
                formatter.format(startTime), formatter.format(endTime)));

        return results.isEmpty() ? List.of("No transaction records during this period.") : results;
    }

    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // Clean up spaces (e.g., full-width)
        timeStr = timeStr.trim().replaceAll("\\s+", " ");

        // If only date provided, append 00:00
        if (timeStr.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            timeStr += " 00:00";
        }

        // Try multiple time formats
        List<String> patterns = Arrays.asList(
                "yyyy/M/d H:mm", "yyyy/M/d HH:mm",
                "yyyy/MM/d H:mm", "yyyy/MM/d HH:mm",
                "yyyy/M/dd H:mm", "yyyy/M/dd HH:mm",
                "yyyy/MM/dd H:mm", "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd"
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String askAi(String prompt) {
        try {
            List<ChatMessage> messages = List.of(
                    ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build()
            );

            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model("ep-20250308174053-7pbkq")
                    .messages(messages)
                    .build();

            return (String) service.createChatCompletion(chatCompletionRequest)
                    .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI request failed: " + e.getMessage();
        }
    }

    public void runAiInThread(String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            String result = analyzeTransactions(userRequest, filePath, startTimeStr, endTimeStr);
            System.out.println("AI analysis result: " + result);
        });
        executor.shutdown();
    }
}

