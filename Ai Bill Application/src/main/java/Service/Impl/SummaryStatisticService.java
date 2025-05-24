package Service.Impl;

import Constants.StandardCategories; // Import StandardCategories
import DAO.TransactionDao; // Import TransactionDao interface
import DAO.UserDao; // Import UserDao interface
import DAO.SummaryStatisticDao; // Import SummaryStatisticDao interface
import model.SummaryStatistic;
import model.Transaction;
import model.User;
import Utils.CacheManager; // Import CacheManager
import Constants.ConfigConstants; // Import ConfigConstants for summary file path

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields; // For getting week of year
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter; // For generating timestamp

public class SummaryStatisticService {

    private final UserDao userDao;
    private final TransactionDao transactionDao; // Need a DAO instance for CacheManager loading
    private final SummaryStatisticDao summaryStatisticDao;
    private final String summaryFilePath;

    /**
     * Constructor to inject dependencies.
     * @param userDao DAO for user data.
     * @param transactionDao DAO for transaction data (used by CacheManager loader).
     * @param summaryStatisticDao DAO for summary statistics data.
     */
    public SummaryStatisticService(UserDao userDao, TransactionDao transactionDao, SummaryStatisticDao summaryStatisticDao) {
        this.userDao = userDao;
        this.transactionDao = transactionDao; // Injected for use in CacheManager loader
        this.summaryStatisticDao = summaryStatisticDao;
        this.summaryFilePath = ConfigConstants.SUMMARY_CSV_PATH; // Get summary file path from config
        System.out.println("SummaryStatisticService initialized. Summary file: " + summaryFilePath);
    }

    /**
     * Helper method to load all transactions from all user files.
     * Uses CacheManager to benefit from caching.
     * @param users List of all users.
     * @return A single list containing all transactions from all users.
     * @throws Exception If loading from any user file fails.
     */
    private List<Transaction> loadAllTransactionsFromAllUsers(List<User> users) throws Exception {
        List<Transaction> allTransactions = new ArrayList<>();
        for (User user : users) {
            String userFilePath = user.getTransactionFilePath();
            if (userFilePath != null && !userFilePath.trim().isEmpty()) {
                try {
                    // Use CacheManager to get transactions for this user's file
                    // Pass the transactionDao instance for the loader
                    List<Transaction> userTransactions = CacheManager.getTransactions(userFilePath, transactionDao);
                    allTransactions.addAll(userTransactions);
                    System.out.println("Loaded " + userTransactions.size() + " transactions for user: " + user.getUsername() + " from " + userFilePath);
                } catch (Exception e) {
                    System.err.println("Error loading transactions for user " + user.getUsername() + " from " + userFilePath + ". Skipping this user.");
                    e.printStackTrace();
                    // Decide whether to stop or continue if one user's file fails.
                    // Continuing is more robust for aggregate statistics.
                    // throw e; // Uncomment to stop processing if any user file fails
                }
            } else {
                System.out.println("User " + user.getUsername() + " has no transaction file path configured. Skipping.");
            }
        }
        return allTransactions;
    }


    /**
     * Helper method to group transactions by week identifier (YYYY-Www).
     * @param transactions The list of transactions.
     * @return A map where keys are week identifiers and values are lists of transactions in that week.
     */
    private Map<String, List<Transaction>> groupTransactionsByWeek(List<Transaction> transactions) {
        WeekFields weekFields = WeekFields.ISO; // ISO 8601 week numbering (Monday is the first day of the week)
        DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("yyyy-'W'ww"); // Format as "YYYY-Www"

        return transactions.stream()
                .filter(t -> t.getTransactionTime() != null && !t.getTransactionTime().trim().isEmpty()) // Filter out transactions with no time
                .collect(Collectors.groupingBy(t -> {
                    try {
                        // Safely parse the transaction date (only date part is needed for week)
                        // Need to ensure the parser is consistent with the one in TransactionServiceImpl/AITransactionService
                        // Let's re-use the safe parsing logic or ensure consistency.
                        // Simplest: Use a helper method for date parsing just for this service, matching expected formats.
                        LocalDate date = parseDateFromTransactionTime(t.getTransactionTime());
                        if (date != null) {
                            return date.format(weekFormatter); // Format date to week identifier
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse date for week grouping: " + t.getTransactionTime() + ". Skipping transaction.");
                        // Transaction with unparseable date will be grouped under 'null' or skipped by filter
                    }
                    return "Unkown week"; // Group unparseable dates under an 'unknown' key
                }));
    }

    // Helper method to parse date from transaction time string (should match other parsers)
    private LocalDate parseDateFromTransactionTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;

        // Clean whitespace and potential hyphens if the expected format is slash-separated
        // Assume the format used in CSV/Transaction model is one of the parsers in other services
        // Let's use a robust set of date patterns matching parseDateTimeSafe in TransactionServiceImpl
        String datePart = timeStr.split(" ")[0]; // Get the date part

        datePart = datePart.trim().replace('-', '/').replaceAll("\\s+", "");


        // Try parsing with multiple slash formats
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
        System.err.println("SummaryStatisticService: Failed to parse date part '" + datePart + "' from transaction time: " + timeStr);
        return null; // Return null if no pattern matches
    }


    /**
     * Retrieves all summary statistics from the data source.
     * @return List of summary statistics.
     * @throws IOException If loading fails.
     */
    public List<SummaryStatistic> getAllSummaryStatistics() throws IOException {
        // Simply delegate to the DAO
        return summaryStatisticDao.loadAllStatistics(summaryFilePath);
    }


    // --- Revised Plan for generateAndSaveWeeklyStatistics ---
    // The previous approach of loading all transactions and then grouping by week
    // doesn't easily allow counting unique users per week unless we augment the Transaction object
    // or wrap it with User info during loading.
    // A better approach for unique user count is to process user by user.

    public void generateAndSaveWeeklyStatistics() throws Exception {
        System.out.println("Generating weekly summary statistics (Revised approach)...");
        List<User> allUsers = userDao.getAllUsers();
        System.out.println("Loaded " + allUsers.size() + " users.");

        // Map to hold weekly stats per user (WeekId -> Map<UserId, UserWeeklyStats>)
        // This intermediate structure is complex.

        // Map to hold aggregated stats for each week (WeekId -> AggregatedWeeklyStats)
        Map<String, Double> totalIncomeByWeek = new HashMap<>();
        Map<String, Double> totalExpenseByWeek = new HashMap<>();
        Map<String, Map<String, Double>> expenseByCategoryByWeek = new HashMap<>(); // WeekId -> (Category -> Amount)
        Map<String, Set<String>> usersByWeek = new HashMap<>(); // WeekId -> Set<Username>

        WeekFields weekFields = WeekFields.ISO;
        DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");


        // Iterate through each user
        for (User user : allUsers) {
            String userFilePath = user.getTransactionFilePath();
            String username = user.getUsername();

            if (userFilePath != null && !userFilePath.trim().isEmpty()) {
                try {
                    // Load transactions for this user
                    List<Transaction> userTransactions = CacheManager.getTransactions(userFilePath, transactionDao);
                    System.out.println("Processing " + userTransactions.size() + " transactions for user: " + username);

                    // Iterate through transactions for this user
                    for (Transaction t : userTransactions) {
                        if (t.getTransactionTime() == null || t.getTransactionTime().trim().isEmpty()) {
                            System.err.println("Skipping transaction with no time for user " + username + ": " + t.getOrderNumber());
                            continue; // Skip transactions with no time
                        }

                        LocalDate date = parseDateFromTransactionTime(t.getTransactionTime());
                        if (date == null) {
                            System.err.println("Skipping transaction with unparseable date for user " + username + ": " + t.getTransactionTime());
                            continue; // Skip transactions with invalid date
                        }

                        String weekIdentifier = date.format(weekFormatter);

                        // Add user to the set for this week
                        usersByWeek.computeIfAbsent(weekIdentifier, k -> new HashSet<>()).add(username);

                        // Aggregate income/expense
                        if (t.getInOut() != null) {
                            String inOut = t.getInOut().trim();
                            if (inOut.equals("Income")) {
                                totalIncomeByWeek.put(weekIdentifier, totalIncomeByWeek.getOrDefault(weekIdentifier, 0.0) + t.getPaymentAmount());
                            } else if (inOut.equals("Expense")) {
                                totalExpenseByWeek.put(weekIdentifier, totalExpenseByWeek.getOrDefault(weekIdentifier, 0.0) + t.getPaymentAmount());

                                // Aggregate expense by standard category
                                String rawType = t.getTransactionType();
                                String standardCategory = StandardCategories.getStandardCategory(rawType);
                                // Only aggregate standard expense categories for the top category calculation
                                if (StandardCategories.isStandardExpenseCategory(standardCategory) || !StandardCategories.ALL_KNOWN_TYPES.contains(standardCategory)) {
                                    String effectiveExpenseCategoryForTop = StandardCategories.isStandardExpenseCategory(standardCategory) ? standardCategory : "其他支出";
                                    expenseByCategoryByWeek.computeIfAbsent(weekIdentifier, k -> new HashMap<>())
                                            .put(effectiveExpenseCategoryForTop, expenseByCategoryByWeek.get(weekIdentifier).getOrDefault(effectiveExpenseCategoryForTop, 0.0) + t.getPaymentAmount());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading transactions for user " + user.getUsername() + " from " + userFilePath + ". Skipping this user's data for statistics.");
                    e.printStackTrace();
                    // Continue processing other users
                }
            } else {
                System.out.println("User " + user.getUsername() + " has no transaction file path configured. Skipping for statistics.");
            }
        }
        System.out.println("Completed aggregation across all users by week.");


        // 5. Consolidate aggregated data into SummaryStatistic objects
        List<SummaryStatistic> calculatedStatistics = new ArrayList<>();
        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(timestampFormatter);

        // Iterate through all week identifiers found
        Set<String> allWeeks = new HashSet<>();
        allWeeks.addAll(totalIncomeByWeek.keySet());
        allWeeks.addAll(totalExpenseByWeek.keySet());
        allWeeks.addAll(usersByWeek.keySet());
        allWeeks.addAll(expenseByCategoryByWeek.keySet());

        List<String> sortedWeekIdentifiers = allWeeks.stream().sorted().collect(Collectors.toList());

        for (String weekIdentifier : sortedWeekIdentifiers) {
            double totalIncome = totalIncomeByWeek.getOrDefault(weekIdentifier, 0.0);
            double totalExpense = totalExpenseByWeek.getOrDefault(weekIdentifier, 0.0);
            int numberOfUsers = usersByWeek.getOrDefault(weekIdentifier, Collections.emptySet()).size();

            // Find top expense category for this week
            Map<String, Double> weeklyExpenseByCategory = expenseByCategoryByWeek.getOrDefault(weekIdentifier, Collections.emptyMap());
            String topExpenseCategory = "无支出";
            double topExpenseCategoryAmount = 0.0;

            Optional<Map.Entry<String, Double>> maxEntry = weeklyExpenseByCategory.entrySet().stream()
                    .max(Map.Entry.comparingByValue());

            if (maxEntry.isPresent()) {
                topExpenseCategory = maxEntry.get().getKey();
                topExpenseCategoryAmount = maxEntry.get().getValue();
            }

            calculatedStatistics.add(new SummaryStatistic(
                    weekIdentifier,
                    totalIncome,
                    totalExpense,
                    topExpenseCategory,
                    topExpenseCategoryAmount,
                    numberOfUsers,
                    timestamp // Timestamp is when the stats were generated, not per week
            ));
        }
        System.out.println("Created " + calculatedStatistics.size() + " SummaryStatistic objects.");


        // 6. Load existing statistics
        List<SummaryStatistic> existingStatistics = getAllSummaryStatistics();
        System.out.println("Loaded " + existingStatistics.size() + " existing summary statistics.");

        // 7. Merge existing and newly calculated statistics (overwrite new weeks, keep old)
        Map<String, SummaryStatistic> finalStatisticsMap = new HashMap<>();
        for(SummaryStatistic stat : existingStatistics) {
            finalStatisticsMap.put(stat.getWeekIdentifier(), stat);
        }
        for(SummaryStatistic stat : calculatedStatistics) {
            finalStatisticsMap.put(stat.getWeekIdentifier(), stat); // New calculation replaces old for the week
        }

        // 8. Sort merged statistics by week identifier (chronologically)
        List<SummaryStatistic> finalStatistics = finalStatisticsMap.values().stream()
                .sorted(Comparator.comparing(SummaryStatistic::getWeekIdentifier))
                .collect(Collectors.toList());

        // 9. Save the final list
        summaryStatisticDao.writeAllStatistics(summaryFilePath, finalStatistics);
        System.out.println("Weekly summary statistics generated and saved successfully to " + summaryFilePath);
    }


}