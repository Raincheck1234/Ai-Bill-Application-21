package model;

// Represents a summary statistic for a specific week across all users
public class SummaryStatistic {
    private String weekIdentifier; // e.g., "2025-W14"
    private double totalIncomeAllUsers;
    private double totalExpenseAllUsers;
    private String topExpenseCategory; // e.g., "FOOD"
    private double topExpenseCategoryAmount; // Amount spent in the top category
    private int numberOfUsersWithTransactions; // Number of users who had any transaction this week
    private String timestampGenerated; // When this statistic record was created

    // Constructors
    public SummaryStatistic() {
    }

    public SummaryStatistic(String weekIdentifier, double totalIncomeAllUsers, double totalExpenseAllUsers, String topExpenseCategory, double topExpenseCategoryAmount, int numberOfUsersWithTransactions, String timestampGenerated) {
        this.weekIdentifier = weekIdentifier;
        this.totalIncomeAllUsers = totalIncomeAllUsers;
        this.totalExpenseAllUsers = totalExpenseAllUsers;
        this.topExpenseCategory = topExpenseCategory;
        this.topExpenseCategoryAmount = topExpenseCategoryAmount;
        this.numberOfUsersWithTransactions = numberOfUsersWithTransactions;
        this.timestampGenerated = timestampGenerated;
    }

    // Getters
    public String getWeekIdentifier() {
        return weekIdentifier;
    }

    public double getTotalIncomeAllUsers() {
        return totalIncomeAllUsers;
    }

    public double getTotalExpenseAllUsers() {
        return totalExpenseAllUsers;
    }

    public String getTopExpenseCategory() {
        return topExpenseCategory;
    }

    public double getTopExpenseCategoryAmount() {
        return topExpenseCategoryAmount;
    }

    public int getNumberOfUsersWithTransactions() {
        return numberOfUsersWithTransactions;
    }

    public String getTimestampGenerated() {
        return timestampGenerated;
    }

    // Setters (if needed for creation/population)
    public void setWeekIdentifier(String weekIdentifier) {
        this.weekIdentifier = weekIdentifier;
    }

    public void setTotalIncomeAllUsers(double totalIncomeAllUsers) {
        this.totalIncomeAllUsers = totalIncomeAllUsers;
    }

    public void setTotalExpenseAllUsers(double totalExpenseAllUsers) {
        this.totalExpenseAllUsers = totalExpenseAllUsers;
    }

    public void setTopExpenseCategory(String topExpenseCategory) {
        this.topExpenseCategory = topExpenseCategory;
    }

    public void setTopExpenseCategoryAmount(double topExpenseCategoryAmount) {
        this.topExpenseCategoryAmount = topExpenseCategoryAmount;
    }

    public void setNumberOfUsersWithTransactions(int numberOfUsersWithTransactions) {
        this.numberOfUsersWithTransactions = numberOfUsersWithTransactions;
    }

    public void setTimestampGenerated(String timestampGenerated) {
        this.timestampGenerated = timestampGenerated;
    }

    @Override
    public String toString() {
        return "SummaryStatistic{" +
                "weekIdentifier='" + weekIdentifier + '\'' +
                ", totalIncomeAllUsers=" + totalIncomeAllUsers +
                ", totalExpenseAllUsers=" + totalExpenseAllUsers +
                ", topExpenseCategory='" + topExpenseCategory + '\'' +
                ", topExpenseCategoryAmount=" + topExpenseCategoryAmount +
                ", numberOfUsersWithTransactions=" + numberOfUsersWithTransactions +
                ", timestampGenerated='" + timestampGenerated + '\'' +
                '}';
    }
}