package DAO.Impl;

import DAO.SummaryStatisticDao;
import model.SummaryStatistic;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvSummaryStatisticDao implements SummaryStatisticDao {

    // Define the header for the summary statistics CSV
    private static final String[] HEADERS = {
            "week_identifier", "total_income_all_users", "total_expense_all_users",
            "top_expense_category", "top_expense_category_amount",
            "number_of_users_with_transactions", "timestamp_generated"
    };

    @Override
    public List<SummaryStatistic> loadAllStatistics(String filePath) throws IOException {
        List<SummaryStatistic> statistics = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path) || Files.size(path) == 0) {
            System.out.println("Summary statistics CSV file not found or is empty: " + filePath);
            return statistics;
        }

        try (Reader reader = new InputStreamReader(
                new BOMInputStream(Files.newInputStream(path)),
                StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase(true)
                    .withTrim(true);

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                Map<String, Integer> headerMap = csvParser.getHeaderMap();
                // Basic header validation
                List<String> requiredHeaders = List.of(HEADERS);
                if (headerMap == null || !headerMap.keySet().containsAll(requiredHeaders)) {
                    throw new IOException("Missing required headers in summary statistics CSV file: " + requiredHeaders +
                            " Found: " + (headerMap == null ? "null" : headerMap.keySet()));
                }

                for (CSVRecord record : csvParser) {
                    try {
                        SummaryStatistic stat = parseRecord(record);
                        if (stat != null) { // parseRecord might return null on error
                            statistics.add(stat);
                        }
                    } catch (Exception e) {
                        System.err.println("Skipping malformed summary statistic record at line " + record.getRecordNumber() + ": " + record.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading summary statistics from CSV: " + filePath);
            e.printStackTrace();
            throw e;
        }
        System.out.println("Successfully loaded " + statistics.size() + " summary statistics from " + filePath);
        return statistics;
    }

    // Helper to parse a single CSV record into a SummaryStatistic object
    private SummaryStatistic parseRecord(CSVRecord record) {
        // Defensive getting of values to prevent exceptions on missing columns
        String weekIdentifier = record.get("week_identifier");
        String totalIncomeStr = record.get("total_income_all_users");
        String totalExpenseStr = record.get("total_expense_all_users");
        String topExpenseCategory = record.get("top_expense_category");
        String topExpenseAmountStr = record.get("top_expense_category_amount");
        String numUsersStr = record.get("number_of_users_with_transactions");
        String timestampGenerated = record.get("timestamp_generated");

        // Basic validation for essential fields
        if (weekIdentifier == null || weekIdentifier.trim().isEmpty() ||
                totalIncomeStr == null || totalIncomeStr.trim().isEmpty() ||
                totalExpenseStr == null || totalExpenseStr.trim().isEmpty() ||
                numUsersStr == null || numUsersStr.trim().isEmpty() ||
                timestampGenerated == null || timestampGenerated.trim().isEmpty()) {
            System.err.println("Skipping summary record due to missing essential fields: " + record.toMap());
            return null; // Indicate parsing failed for this record
        }

        try {
            double totalIncome = Double.parseDouble(totalIncomeStr.trim());
            double totalExpense = Double.parseDouble(totalExpenseStr.trim());
            double topExpenseAmount = (topExpenseAmountStr != null && !topExpenseAmountStr.trim().isEmpty()) ? Double.parseDouble(topExpenseAmountStr.trim()) : 0.0;
            int numUsers = Integer.parseInt(numUsersStr.trim());

            return new SummaryStatistic(
                    weekIdentifier.trim(),
                    totalIncome,
                    totalExpense,
                    topExpenseCategory != null ? topExpenseCategory.trim() : "N/A", // Top category might be empty if no expenses
                    topExpenseAmount,
                    numUsers,
                    timestampGenerated.trim()
            );
        } catch (NumberFormatException e) {
            System.err.println("Skipping summary record due to number format error: " + record.toMap());
            e.printStackTrace();
            return null; // Indicate parsing failed
        }
    }


    @Override
    public void writeAllStatistics(String filePath, List<SummaryStatistic> statistics) throws IOException {
        Path path = Paths.get(filePath);
        // Ensure the directory exists
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        // Use a temporary file for atomic write
        File targetFile = path.toFile();
        File tempFile = File.createTempFile("summary_temp", ".csv", targetFile.getParentFile());

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(HEADERS).withTrim())) { // Always write header for overwrite

            for (SummaryStatistic stat : statistics) {
                csvPrinter.printRecord(
                        stat.getWeekIdentifier(),
                        stat.getTotalIncomeAllUsers(),
                        stat.getTotalExpenseAllUsers(),
                        stat.getTopExpenseCategory(),
                        stat.getTopExpenseCategoryAmount(),
                        stat.getNumberOfUsersWithTransactions(),
                        stat.getTimestampGenerated()
                );
            }
            // csvPrinter.flush(); // Auto-flushed on close
        } catch (IOException e) {
            tempFile.delete(); // Clean up temp file on failure
            System.err.println("Error writing summary statistics to temporary CSV file: " + tempFile.toPath());
            e.printStackTrace();
            throw e; // Re-throw
        }

        // Atomic replacement
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Atomically replaced " + filePath + " with updated summary statistics.");
        } catch (IOException e) {
            System.err.println("Failed to atomically replace original summary file: " + targetFile.toPath());
            tempFile.delete(); // Clean up temp file
            e.printStackTrace();
            throw e; // Re-throw
        }
    }
}