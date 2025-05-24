package Constants;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections; // For immutable lists
import java.util.stream.Collectors;

public final class StandardCategories {
    private StandardCategories() {} // Prevent instantiation

    // Standard Expense Categories for analysis and filtering
    public static final List<String> EXPENSE_CATEGORIES = Collections.unmodifiableList(List.of(
            "Dining",
            "Groceries",
            "Clothing",
            "Daily Necessities",
            "Transportation",
            "Entertainment",
            "Housing",
            "Communication",
            "Education",
            "Medical",
            "Financial Services",
            "Other Expenses"      //  General catch-all for expenses
    ));

    // Standard Income Categories
    public static final List<String> INCOME_CATEGORIES = Collections.unmodifiableList(List.of(
            "Salary",
            "Part-time Income",
            "Investment Income",
            "Other Income"        //  General catch-all for income
    ));

    // Special Transaction Types (may not be strictly 'expense' or 'income' in analysis)
    public static final List<String> SPECIAL_TYPES = Collections.unmodifiableList(List.of(
            "Transfer",           //  Transfers between accounts or people
            "Red Packet"          //  WeChat/Alipay Red Packets - often social, not regular income/expense
            // Add other special types as needed, e.g., Refund, Credit Card Repayment
    ));

    /**
     * Generates a comma-separated string of all standard categories for AI prompts.
     * @return String like "Dining,Groceries,Clothing,..."
     */
    public static String getAllCategoriesString() {
        return ALL_KNOWN_TYPES.stream()
                .collect(Collectors.joining(","));
    }

    // All Known Transaction Types (combination of all the above + potentially user-defined ones initially)
    // This list might be used for dropdowns in UI, etc.
    public static final List<String> ALL_KNOWN_TYPES;

    static {
        List<String> allTypesMutable = new ArrayList<>();
        allTypesMutable.addAll(EXPENSE_CATEGORIES);
        allTypesMutable.addAll(INCOME_CATEGORIES);
        allTypesMutable.addAll(SPECIAL_TYPES);
        ALL_KNOWN_TYPES = Collections.unmodifiableList(allTypesMutable);
    }


    /**
     * Helper method to check if a transaction type is a standard expense category.
     * Handles potential variations like "支" vs "支出".
     */
    public static boolean isStandardExpenseCategory(String type) {
        if (type == null) return false;
        String trimmedType = type.trim();
        // First, check if it's directly in the list
        if (EXPENSE_CATEGORIES.contains(trimmedType)) {
            return true;
        }
        // Handle common aliases/variations if necessary, e.g., "交通费" -> "交通"
        // This might require a mapping logic if aliases are common and not handled by AI mapping.
        return false; // For now, strict check
    }

    /**
     * Helper method to get the standardized category for a given transaction type string.
     * This is where mapping from potentially messy user input or AI output to standard categories happens.
     * For now, a simple direct match is used. In the future, this could use AI recognition results
     * or manual mapping rules.
     * @param rawType The transaction type string from data.
     * @return The matched standard category, or the original rawType if no standard match, or "Unknown" for null/empty.
     */
    public static String getStandardCategory(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return "Unknown"; // Unknown category
        }
        String trimmedType = rawType.trim();
        // Check if it matches any standard category (case-insensitive might be better)
        // Let's do a simple case-sensitive check for now against the predefined lists.
        if (ALL_KNOWN_TYPES.contains(trimmedType)) {
            return trimmedType; // Direct match
        }

        // Future Improvement: Implement smarter mapping here, potentially using AI suggestions
        // or a configuration file for mapping common user inputs to standard categories.


        return trimmedType; // If no standard match, return the original type.
        // We might want a dedicated "Other" if it doesn't fit any *known* type.
    }

    /**
     * Helper method to check if a transaction type is a standard income category.
     */
    public static boolean isStandardIncomeCategory(String type) {
        if (type == null) return false;
        return INCOME_CATEGORIES.contains(type.trim());
    }

    /**
     * Helper method to check if a transaction type is a special type.
     */
    public static boolean isSpecialType(String type) {
        if (type == null) return false;
        return SPECIAL_TYPES.contains(type.trim());
    }


}