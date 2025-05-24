package model;

import java.util.HashMap;
import java.util.Map;

// Represents a summary of transactions for a specific month
public class MonthlySummary {
    private String monthIdentifier; // e.g., "2025-03"
    private double totalIncome;
    private double totalExpense;
    // Map from standard expense category to total amount spent in that category
    private Map<String, Double> expenseByCategory;

    public MonthlySummary(String monthIdentifier) {
        this.monthIdentifier = monthIdentifier;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
        this.expenseByCategory = new HashMap<>();
    }

    // Getters
    public String getMonthIdentifier() {
        return monthIdentifier;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public Map<String, Double> getExpenseByCategory() {
        return expenseByCategory;
    }

    // Methods to add transaction amounts
    public void addIncome(double amount) {
        this.totalIncome += amount;
    }

    public void addExpense(double amount, String standardCategory) {
        this.totalExpense += amount;
        // Aggregate by standard category
        expenseByCategory.put(standardCategory, expenseByCategory.getOrDefault(standardCategory, 0.0) + amount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Month: ").append(monthIdentifier).append("\n");
        sb.append("  Total Income: ").append(String.format("%.2f", totalIncome)).append("元\n");
        sb.append("  Total Expense: ").append(String.format("%.2f", totalExpense)).append("元\n");
        sb.append("  Expenses by Category:\n");
        if (expenseByCategory.isEmpty()) {
            sb.append("    (None)\n");
        } else {
            // Sort categories alphabetically for consistent output
            expenseByCategory.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                            sb.append(String.format("    %s: %.2fCNY\n", entry.getKey(), entry.getValue()))
                    );
        }
        return sb.toString();
    }
}