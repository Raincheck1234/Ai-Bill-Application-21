package Service.AIservice;

// ... existing imports ...
import Constants.StandardCategories;
import DAO.TransactionDao;
import DAO.Impl.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;
import Utils.CacheManager;
import model.Transaction;
import model.MonthlySummary; // Import MonthlySummary
import Service.TransactionService; // Import TransactionService interface


import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Import Map
import java.util.Collections; // For sorting map entries
import java.util.Optional;
import java.util.stream.Collectors;


public class CollegeStudentNeeds {
    // Keep existing prompts, maybe refine them to mention using the data provided
    private final String requestBudge="我是一名预算有限的大学生，请根据下面我给出的历史周花费和月度收支总结，帮助我给下周预算范围，必须以[最低预算，最高预算],的方式给出回答，不能有多余的回复。";
    private final String requestTips="我是一名预算有限的大学生，请结合下面我给出的月度消费总结数据，为我推荐一些有针对性的省钱方法。"; // Refined prompt
    // Add the missing constant:
    private final String requestRecognition =
            "请根据以下账单信息推测最合适的交易类型。返回的类型必须精确匹配以下列表中的一个条目：\n" +
                    StandardCategories.getAllCategoriesString() + "\n" + // Include the list of valid categories
                    "如果无法确定，请返回 '其他支出' 或 '其他收入'（取决于收支方向）。只返回类型字符串，不要包含额外文本或解释。账单信息：";
    // AITransactionService is used for asking AI, can be an instance or created on demand
    // private final AITransactionService aiService = new AITransactionService(); // This instance won't have injected TransactionService

    // Need a way to get TransactionService here to generate monthly summaries
    // Option 1: Inject TransactionService into CollegeStudentNeeds constructor
    private final TransactionService transactionService; // Inject TransactionService

    /**
     * Constructor now accepts TransactionService instance.
     */
    public CollegeStudentNeeds(TransactionService transactionService) {
        this.transactionService = transactionService; // Inject the service
        System.out.println("CollegeStudentNeeds initialized with TransactionService.");
    }


    /**
     * Recognizes the spending category of a single transaction using AI. (Keep as is, uses raw transaction)
     *
     * @param transaction The transaction to recognize.
     * @return The AI's suggested category.
     */
    public String RecognizeTransaction(Transaction transaction){
        if (transaction == null) {
            return "无法识别空交易信息";
        }
        StringBuilder sb=new StringBuilder();
        sb.append("交易类型:").append(transaction.getTransactionType()).append(",")
                .append("交易对方:").append(transaction.getCounterparty()).append(",")
                .append("商品:").append(transaction.getCommodity()).append(",")
                .append("收/支:").append(transaction.getInOut()).append(",")
                .append("金额(元):").append(String.format("%.2f", transaction.getPaymentAmount())).append(",") // Format amount
                .append("支付方式:").append(transaction.getPaymentMethod()).append(",")
                .append("备注:").append(transaction.getRemarks());

        System.out.println("CollegeStudentNeeds: Sending recognition request to AI: " + sb.toString());
        // Need a separate AITransactionService instance or method call that doesn't depend on injected TransactionService
        // Option 2: Create a local AITransactionService instance just for askAi calls
        AITransactionService localAiService = new AITransactionService(null); // Pass null for TransactionService as it's not needed by askAi
        return  localAiService.askAi(requestRecognition + sb.toString());
    }

    /**
     * Generates saving tips for college students using AI, now based on monthly summary.
     * @param userFilePath The path to the user's transaction CSV file. (Not strictly needed if service is user-scoped)
     * @return AI's suggested saving tips.
     */
    // Refined to use monthly summary data
    public String generateTipsForSaving(String userFilePath){ // Keep filePath parameter for consistency or remove if service is user-scoped
        try {
            // Get monthly summary data from TransactionService
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary(); // Use injected service
            System.out.println("CollegeStudentNeeds: Retrieved " + summaries.size() + " months of summary data for tips.");

            if (summaries.isEmpty()) {
                return "没有找到足够的交易数据来提供个性化节约建议。";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(requestTips).append("\n\n以下是我的月度消费总结数据：\n\n");

            // Sort months chronologically
            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  总支出: ").append(String.format("%.2f", ms.getTotalExpense())).append("元\n");
                promptBuilder.append("  支出明细:\n");
                if (ms.getExpenseByCategory().isEmpty()) {
                    promptBuilder.append("    (无支出)\n");
                } else {
                    // Sort categories by amount descending
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f元\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }

            String aiPrompt = promptBuilder.toString();
            System.out.println("CollegeStudentNeeds: Sending saving tips prompt to AI. Prompt length: " + aiPrompt.length());

            // Need a separate AITransactionService instance or method call that doesn't depend on injected TransactionService
            AITransactionService localAiService = new AITransactionService(null); // Pass null for TransactionService
            return localAiService.askAi(aiPrompt);

        } catch (Exception e) {
            System.err.println("CollegeStudentNeeds: Failed to generate saving tips.");
            e.printStackTrace();
            return "生成个性化节约建议失败: " + e.getMessage();
        }
    }

    /**
     * Analyzes weekly spending and asks AI for a budget range, now also includes monthly summary context.
     * @param filePath The path to the user's transaction CSV file.
     * @return A double array [minBudget, maxBudget] parsed from AI response, or [-1, -1] on failure.
     * @throws Exception If there's an error accessing the transaction data.
     */
    // Inside CollegeStudentNeeds class, modify the generateBudget method:

    public double[] generateBudget(String filePath) throws Exception {
        List<Transaction> transactions;
        Map<String, MonthlySummary> summaries; // Declare the summaries variable here

        try {
            // Get transactions using CacheManager
            TransactionDao transactionDaoForLoading = new CsvTransactionDao();
            transactions = CacheManager.getTransactions(filePath, transactionDaoForLoading);
            System.out.println("CollegeStudentNeeds: Retrieved " + transactions.size() + " transactions for budget analysis from: " + filePath);

            // Get monthly summary data for context
            summaries = transactionService.getMonthlyTransactionSummary(); // Assign to the declared variable
            System.out.println("CollegeStudentNeeds: Retrieved " + summaries.size() + " months of summary data for budget context.");

        } catch (Exception e) {
            System.err.println("CollegeStudentNeeds: Error retrieving transactions or summary for budget analysis: " + filePath);
            e.printStackTrace();
            throw e;
        }

        int size = transactions.size();
        if (size == 0) {
            System.out.println("CollegeStudentNeeds: No transactions found for budget analysis.");
            // If no transactions, prompt AI using only the monthly summary (if available)
            if (!summaries.isEmpty()) {
                // Build prompt with only monthly summary
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("以下是我的月度收支总结数据：\n\n");
                // ... (Code to format monthly summaries for prompt - copy from generatePersonalSummary or generateTipsForSaving) ...
                List<String> sortedMonths = new ArrayList<>(summaries.keySet());
                Collections.sort(sortedMonths);
                for (String month : sortedMonths) {
                    MonthlySummary ms = summaries.get(month);
                    promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                    promptBuilder.append("  总收入: ").append(String.format("%.2f", ms.getTotalIncome())).append("元\n");
                    promptBuilder.append("  总支出: ").append(String.format("%.2f", ms.getTotalExpense())).append("元\n");
                    double net = ms.getTotalIncome() - ms.getTotalExpense();
                    promptBuilder.append("  月度净收支: ").append(String.format("%.2f", net)).append("元\n");
                    promptBuilder.append("  主要支出类别:\n"); // Include expense category details
                    if (ms.getExpenseByCategory().isEmpty()) {
                        promptBuilder.append("    (无支出)\n");
                    } else {
                        ms.getExpenseByCategory().entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                .forEach(entry ->
                                        promptBuilder.append(String.format("    %s: %.2f元\n", entry.getKey(), entry.getValue()))
                                );
                    }
                    promptBuilder.append("\n");
                }
                // Use the local AI service instance to ask AI
                String answer = new AITransactionService(null).askAi(requestBudge + "\n\n没有找到周支出数据。\n" + promptBuilder.toString());
                return parseDoubleArrayFromString(answer);

            }
            return new double[]{-1, -1}; // No data at all
        }

        // Filter for '支出' transactions and sort them by date (newest first)
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getInOut() != null && (t.getInOut().equals("支出") || t.getInOut().equals("支")))
                .sorted((t1, t2) -> {
                    LocalDate date1 = parseDateSafe(t1.getTransactionTime());
                    LocalDate date2 = parseDateSafe(t2.getTransactionTime());
                    if (date1 != null && date2 != null) { return date2.compareTo(date1); } else if (date1 == null && date2 == null) { return 0; } else if (date1 == null) { return 1; } else { return -1; }
                })
                .collect(Collectors.toList());
        System.out.println("CollegeStudentNeeds: Filtered " + expenseTransactions.size() + " expense transactions for budget analysis.");


        if (expenseTransactions.isEmpty()) {
            System.out.println("CollegeStudentNeeds: No expense transactions found for budget analysis.");
            // Still provide monthly summary context to AI if available
            if (!summaries.isEmpty()) {
                // Build prompt with only monthly summary
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("以下是我的月度收支总结数据：\n\n");
                // ... (Code to format monthly summaries for prompt - copy from above) ...
                List<String> sortedMonths = new ArrayList<>(summaries.keySet());
                Collections.sort(sortedMonths);
                for (String month : sortedMonths) {
                    MonthlySummary ms = summaries.get(month);
                    promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                    promptBuilder.append("  总收入: ").append(String.format("%.2f", ms.getTotalIncome())).append("元\n");
                    promptBuilder.append("  总支出: ").append(String.format("%.2f", ms.getTotalExpense())).append("元\n");
                    double net = ms.getTotalIncome() - ms.getTotalExpense();
                    promptBuilder.append("  月度净收支: ").append(String.format("%.2f", net)).append("元\n");
                    promptBuilder.append("  主要支出类别:\n"); // Include expense category details
                    if (ms.getExpenseByCategory().isEmpty()) {
                        promptBuilder.append("    (无支出)\n");
                    } else {
                        ms.getExpenseByCategory().entrySet().stream()
                                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                .forEach(entry ->
                                        promptBuilder.append(String.format("    %s: %.2f元\n", entry.getKey(), entry.getValue()))
                                );
                    }
                    promptBuilder.append("\n");
                }
                String answer = new AITransactionService(null).askAi(requestBudge + "\n\n没有找到周支出数据。\n" + promptBuilder.toString());
                return parseDoubleArrayFromString(answer);
            }
            return new double[]{-1, -1}; // No expense data and no summary data
        }

        // --- Weekly Expense Calculation (Keep existing logic) ---
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
        if (currentWeekTotal > 0 || currentWeekStart != null) {
            weeklyExpenses.add(currentWeekTotal);
        }
        System.out.println("CollegeStudentNeeds: Calculated weekly expenses for " + weeklyExpenses.size() + " weeks: " + weeklyExpenses);


        // --- Format Prompt including Weekly Expenses and Monthly Summary ---
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(requestBudge).append("\n\n"); // Start with the budget request prompt

        // Add Weekly Expenses Section
        promptBuilder.append("以下是我最近的每周花费数据：\n");
        if (weeklyExpenses.isEmpty()) {
            promptBuilder.append("(没有找到足够周期的支出数据)\n");
        } else {
            for (int i = 0; i < weeklyExpenses.size(); i++) {
                promptBuilder.append("第");
                promptBuilder.append(weeklyExpenses.size() - 1 - i);
                promptBuilder.append("周:花费");
                promptBuilder.append(String.format("%.2f", weeklyExpenses.get(i)));
                promptBuilder.append("元; ");
            }
            promptBuilder.append("\n");
        }

        // Add Monthly Summary Section
        promptBuilder.append("\n同时，以下是我的月度收支总结数据：\n\n");
        if (summaries.isEmpty()) {
            promptBuilder.append("(没有找到月度总结数据)\n");
        } else {
            List<String> sortedMonths = new ArrayList<>(summaries.keySet());
            Collections.sort(sortedMonths);

            for (String month : sortedMonths) {
                MonthlySummary ms = summaries.get(month);
                promptBuilder.append("--- ").append(ms.getMonthIdentifier()).append(" ---\n");
                promptBuilder.append("  总收入: ").append(String.format("%.2f", ms.getTotalIncome())).append("元\n");
                promptBuilder.append("  总支出: ").append(String.format("%.2f", ms.getTotalExpense())).append("元\n");
                double net = ms.getTotalIncome() - ms.getTotalExpense();
                promptBuilder.append("  月度净收支: ").append(String.format("%.2f", net)).append("元\n");
                promptBuilder.append("  主要支出类别:\n"); // Include expense category details
                if (ms.getExpenseByCategory().isEmpty()) {
                    promptBuilder.append("    (无支出)\n");
                } else {
                    ms.getExpenseByCategory().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                            .forEach(entry ->
                                    promptBuilder.append(String.format("    %s: %.2f元\n", entry.getKey(), entry.getValue()))
                            );
                }
                promptBuilder.append("\n");
            }
        }


        String aiPrompt = promptBuilder.toString(); // This now contains the full prompt
        System.out.println("CollegeStudentNeeds: Sending budget request to AI. Prompt length: " + aiPrompt.length());

        // Use the local AI service instance to ask AI
        String answer = new AITransactionService(null).askAi(aiPrompt); // Pass null as askAi doesn't need TransactionService
        System.out.println("CollegeStudentNeeds: Received budget response from AI: " + answer);

        double[] ret = parseDoubleArrayFromString(answer);
        if (ret == null || ret.length != 2) {
            System.err.println("CollegeStudentNeeds: Failed to parse budget array from AI response: " + answer + ". Full AI Response: " + answer); // Log the full response
            return new double[]{-1, -1};
        }

        return ret;
    }

    // Helper method to safely parse date from transaction time string (must be consistent!)
    private LocalDate parseDateSafe(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // Clean whitespace and potential hyphens if the expected format is slash-separated
        String datePart = timeStr.split(" ")[0]; // Get the date part

        datePart = datePart.trim().replace('-', '/').replaceAll("\\s+", "");

        List<String> patterns = List.of(
                "yyyy/M/d", "yyyy/MM/d", "yyyy/M/dd", "yyyy/MM/dd",
                "yyyy-MM-dd" // Add dash format just in case
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(datePart, formatter);
            } catch (Exception ignored) {
                // Ignore parsing errors for this pattern
            }
        }
        System.err.println("CollegeStudentNeeds: Failed to parse date part '" + datePart + "' from transaction time: " + timeStr);
        return null; // Return null if no pattern matches
    }

    // Keep parseDoubleArrayFromString method - ensure robustness
    public double[] parseDoubleArrayFromString(String input) {
        // ... existing robust implementation ...
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