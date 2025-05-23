package Service.Impl;

import DAO.Impl.CsvSummaryStatisticDao;
import DAO.Impl.CsvTransactionDao;
import DAO.Impl.CsvUserDao;
import DAO.SummaryStatisticDao;
import DAO.TransactionDao;
import DAO.UserDao;
import model.SummaryStatistic;
import Constants.ConfigConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SummaryStatisticServiceTest {

    private SummaryStatisticService summaryStatisticService;
    private Path tempSummaryFilePath;
    private final String originalSummaryPath = ConfigConstants.SUMMARY_CSV_PATH;

    // For generateAndSaveWeeklyStatistics, we also need user and transaction DAOs and their data
    private UserDao userDao;
    private TransactionDao transactionDao; // This DAO is used by CacheManager within SummaryStatisticService

    @BeforeEach
    void setUp() throws IOException {
        userDao = new CsvUserDao(ConfigConstants.USERS_CSV_PATH);
        transactionDao = new CsvTransactionDao(); // For CacheManager
        SummaryStatisticDao summaryDao = new CsvSummaryStatisticDao();

        summaryStatisticService = new SummaryStatisticService(userDao, transactionDao, summaryDao);

        // Create a temporary copy of the summary statistics file
        Path originalPathFile = Paths.get(originalSummaryPath);
        if (!Files.exists(originalPathFile)) {
            Files.createDirectories(originalPathFile.getParent());
            Files.createFile(originalPathFile);
            Files.writeString(originalPathFile, "week_identifier,total_income_all_users,total_expense_all_users,top_expense_category,top_expense_category_amount,number_of_users_with_transactions,timestamp_generated\n");
            System.out.println("SummaryStatisticServiceTest: Created dummy original summary file as it was missing: " + originalSummaryPath);
        }
        tempSummaryFilePath = Files.createTempFile("test_summary_service_", ".csv");
        Files.copy(originalPathFile, tempSummaryFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Override the summaryFilePath in the service to use the temp file for write operations
        // This requires SummaryStatisticService to allow setting the path, or we test against the original.
        // For simplicity, generateAndSaveWeeklyStatistics will write to the original path configured in ConfigConstants.
        // So, for that test, we'll back up the original, run the test, then restore.
        // For getAllSummaryStatistics, we can read from the original.
        System.out.println("SummaryStatisticServiceTest: Setup complete. Temp summary file for some tests: " + tempSummaryFilePath);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempSummaryFilePath != null && Files.exists(tempSummaryFilePath)) {
            Files.delete(tempSummaryFilePath);
            System.out.println("SummaryStatisticServiceTest: Deleted temp summary file: " + tempSummaryFilePath);
        }
    }

    @Test
    void testGetAllSummaryStatistics() {
        System.out.println("SummaryStatisticServiceTest: Running testGetAllSummaryStatistics...");
        try {
            // This will read from ConfigConstants.SUMMARY_CSV_PATH
            List<SummaryStatistic> stats = summaryStatisticService.getAllSummaryStatistics();
            System.out.println("SummaryStatisticServiceTest (getAll): Loaded " + stats.size() + " summary statistics.");
            if (!stats.isEmpty()) {
                System.out.println("SummaryStatisticServiceTest (getAll): First stat week: " + stats.get(0).getWeekIdentifier());
            }
        } catch (Exception e) {
            System.err.println("SummaryStatisticServiceTest (getAll): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("SummaryStatisticServiceTest: testGetAllSummaryStatistics finished.");
    }

    @Test
    void testGenerateAndSaveWeeklyStatistics() {
        System.out.println("SummaryStatisticServiceTest: Running testGenerateAndSaveWeeklyStatistics...");
        Path backupPath = null;
        Path originalPath = Paths.get(ConfigConstants.SUMMARY_CSV_PATH);
        try {
            // Backup the original summary_statistics.csv
            if (Files.exists(originalPath)) {
                backupPath = Paths.get(originalPath.toString() + ".bak");
                Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("SummaryStatisticServiceTest (generate): Backed up original summary to " + backupPath);
            }

            summaryStatisticService.generateAndSaveWeeklyStatistics();
            System.out.println("SummaryStatisticServiceTest (generate): generateAndSaveWeeklyStatistics completed.");

            // Optionally, load and print some of the generated stats to verify
            List<SummaryStatistic> generatedStats = summaryStatisticService.getAllSummaryStatistics();
            System.out.println("SummaryStatisticServiceTest (generate): Found " + generatedStats.size() + " stats after generation.");
            if(!generatedStats.isEmpty()){
                System.out.println("SummaryStatisticServiceTest (generate): Sample generated stat week: " + generatedStats.get(generatedStats.size()-1).getWeekIdentifier() + " with " + generatedStats.get(generatedStats.size()-1).getNumberOfUsersWithTransactions() + " users.");
            }

        } catch (Exception e) {
            System.err.println("SummaryStatisticServiceTest (generate): Error.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            // Restore the original summary_statistics.csv
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    Files.move(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("SummaryStatisticServiceTest (generate): Restored original summary from " + backupPath);
                } catch (IOException e) {
                    System.err.println("SummaryStatisticServiceTest (generate): Error restoring backup.");
                    e.printStackTrace();
                }
            } else if (backupPath == null && Files.exists(originalPath) && !originalSummaryPath.equals(ConfigConstants.SUMMARY_CSV_PATH)) {
                // If no backup was made (original didn't exist), but test created one, delete it.
                // This case is less likely with current setup.
                try {
                    // Files.delete(originalPath);
                    // System.out.println("SummaryStatisticServiceTest (generate): Deleted summary file created by test.");
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        System.out.println("SummaryStatisticServiceTest: testGenerateAndSaveWeeklyStatistics finished.");
    }
}