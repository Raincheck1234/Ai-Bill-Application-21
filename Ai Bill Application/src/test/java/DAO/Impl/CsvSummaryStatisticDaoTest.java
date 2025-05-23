package DAO.Impl;


import DAO.Impl.CsvSummaryStatisticDao;
import DAO.SummaryStatisticDao;
import model.SummaryStatistic;
import Constants.ConfigConstants; // For SUMMARY_CSV_PATH

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CsvSummaryStatisticDaoTest {

    private SummaryStatisticDao summaryDao;
    private final String sampleSummaryFilePath = ConfigConstants.SUMMARY_CSV_PATH;
    private Path tempSummaryFilePath;

    @BeforeEach
    void setUp() throws IOException {
        summaryDao = new CsvSummaryStatisticDao();
        Path originalPath = Paths.get(sampleSummaryFilePath);
        if (!Files.exists(originalPath)) {
            // Create a dummy original file if it doesn't exist, so copy works
            Files.createFile(originalPath);
            // Optionally write a header or a dummy record
            Files.writeString(originalPath, "week_identifier,total_income_all_users,total_expense_all_users,top_expense_category,top_expense_category_amount,number_of_users_with_transactions,timestamp_generated\n");
            System.out.println("CsvSummaryStatisticDaoTest: Created dummy summary file at " + sampleSummaryFilePath + " because it was missing.");
        }
        tempSummaryFilePath = Files.createTempFile("test_summary_stats_", ".csv");
        Files.copy(originalPath, tempSummaryFilePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("CsvSummaryStatisticDaoTest: Copied " + sampleSummaryFilePath + " to temporary file " + tempSummaryFilePath.toString() + " for testing.");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempSummaryFilePath != null && Files.exists(tempSummaryFilePath)) {
            Files.delete(tempSummaryFilePath);
            System.out.println("CsvSummaryStatisticDaoTest: Deleted temporary summary file " + tempSummaryFilePath.toString());
        }
    }

    @Test
    void testLoadAllStatistics() {
        try {
            // Test loading from the original sample file
            List<SummaryStatistic> stats = summaryDao.loadAllStatistics(sampleSummaryFilePath);
            System.out.println("CsvSummaryStatisticDaoTest (loadAll): Loaded " + stats.size() + " summary statistics from " + sampleSummaryFilePath);
            if (!stats.isEmpty()) {
                System.out.println("CsvSummaryStatisticDaoTest (loadAll): First statistic: " + stats.get(0).getWeekIdentifier());
            }
        } catch (Exception e) {
            System.err.println("CsvSummaryStatisticDaoTest (loadAll): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvSummaryStatisticDaoTest (loadAll): testLoadAllStatistics finished.");
    }

    @Test
    void testWriteAllStatistics() {
        try {
            List<SummaryStatistic> newStats = new ArrayList<>();
            newStats.add(new SummaryStatistic("2024-W01", 1000, 500, "Food", 200, 10, "2024-01-08 10:00:00"));
            newStats.add(new SummaryStatistic("2024-W02", 1200, 600, "Transport", 150, 12, "2024-01-15 10:00:00"));

            System.out.println("CsvSummaryStatisticDaoTest (writeAll): Attempting to write " + newStats.size() + " new statistics to " + tempSummaryFilePath.toString());
            summaryDao.writeAllStatistics(tempSummaryFilePath.toString(), newStats);
            System.out.println("CsvSummaryStatisticDaoTest (writeAll): Statistics written.");

            List<SummaryStatistic> reReadStats = summaryDao.loadAllStatistics(tempSummaryFilePath.toString());
            System.out.println("CsvSummaryStatisticDaoTest (writeAll): Re-read " + reReadStats.size() + " statistics.");
            if (reReadStats.size() == newStats.size()) {
                System.out.println("CsvSummaryStatisticDaoTest (writeAll): Count matches.");
                if(!reReadStats.isEmpty()){
                    System.out.println("CsvSummaryStatisticDaoTest (writeAll): First re-read stat week: " + reReadStats.get(0).getWeekIdentifier());
                }
            } else {
                System.err.println("CsvSummaryStatisticDaoTest (writeAll): Count MISMATCH after write/read.");
            }

        } catch (Exception e) {
            System.err.println("CsvSummaryStatisticDaoTest (writeAll): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("CsvSummaryStatisticDaoTest (writeAll): testWriteAllStatistics finished.");
    }
}