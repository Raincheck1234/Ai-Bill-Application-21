package Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration Constants Class (Thread-safe initialization)
 */
public final class ConfigConstants {
    private ConfigConstants() {} // Private constructor to prevent instantiation

    // CSV Path Constants
    public static final String CSV_PATH; // Original, may still be referenced in old code
    public static final String USERS_CSV_PATH; // User CSV path
    public static final String SUMMARY_CSV_PATH; // Added summary statistics CSV path


    // Static initialization block (executes when the class is loaded)
    static {
        Properties prop = new Properties();
        try (InputStream input = ConfigConstants.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("Configuration file config.properties not found in classpath");
            }

            prop.load(input);

            CSV_PATH = prop.getProperty("csv.path");
            USERS_CSV_PATH = prop.getProperty("csv.users_path");
            SUMMARY_CSV_PATH = prop.getProperty("csv.summary_path"); // Read summary statistics CSV path


            // Basic validation
            if (USERS_CSV_PATH == null || USERS_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.users_path' not found or is empty in config.properties.");
            }
            if (SUMMARY_CSV_PATH == null || SUMMARY_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.summary_path' not found or is empty in config.properties.");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file", e); // Convert to runtime exception
        }
        System.out.println("Loaded USERS_CSV_PATH: " + USERS_CSV_PATH);
        System.out.println("Loaded SUMMARY_CSV_PATH: " + SUMMARY_CSV_PATH);
        if (CSV_PATH != null) System.out.println("Loaded CSV_PATH: " + CSV_PATH); // Optional print
    }
}