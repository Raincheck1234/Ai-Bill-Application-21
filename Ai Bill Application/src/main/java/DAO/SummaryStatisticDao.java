package DAO;

import model.SummaryStatistic;

import java.io.IOException;
import java.util.List;

public interface SummaryStatisticDao {

    /**
     * Loads all summary statistics from the configured data source.
     * @param filePath The path to the summary statistics CSV file.
     * @return A list of all summary statistics.
     * @throws IOException If an I/O error occurs during loading.
     */
    List<SummaryStatistic> loadAllStatistics(String filePath) throws IOException;

    /**
     * Writes a list of summary statistics to the configured data source, overwriting existing data.
     * @param filePath The path to the summary statistics CSV file.
     * @param statistics The list of statistics to write.
     * @throws IOException If an I/O error occurs during saving.
     */
    void writeAllStatistics(String filePath, List<SummaryStatistic> statistics) throws IOException;

    // Optional: Add method to get statistic by week identifier if needed
    // SummaryStatistic getStatisticByWeek(String filePath, String weekIdentifier) throws IOException;
}