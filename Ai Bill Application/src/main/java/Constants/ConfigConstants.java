package Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration constants (thread-safe initialization).
 */
public final class ConfigConstants {
    private ConfigConstants() {} // Private constructor to prevent instantiation

    // CSV Path constants
    public static final String CSV_PATH; // Original, may still be referenced in old code
    public static final String USERS_CSV_PATH; // User CSV path
    public static final String SUMMARY_CSV_PATH; // Example main path (for admin global stats?)
    public static final String USER_DATA_BASE_DIR; // NEW: Base directory for user data files


    // Static initialization block (executed when class is loaded)
    static {
        Properties prop = new Properties();
        // Load config.properties from classpath
        try (InputStream input = ConfigConstants.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("Configuration file config.properties not found in classpath");
            }

            prop.load(input);

            // Load values from properties
            CSV_PATH = prop.getProperty("csv.path");
            USERS_CSV_PATH = prop.getProperty("csv.users_path");
            SUMMARY_CSV_PATH = prop.getProperty("csv.summary_path");
            USER_DATA_BASE_DIR = prop.getProperty("user.data.base.dir"); // Load new config


            // Basic validation for critical paths
            if (USERS_CSV_PATH == null || USERS_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.users_path' not found or is empty in config.properties.");
            }
            // SUMMARY_CSV_PATH might not be critical if only for admin global stats and not used by users
            if (USER_DATA_BASE_DIR == null || USER_DATA_BASE_DIR.trim().isEmpty()) {
                throw new RuntimeException("'user.data.base.dir' not found or is empty in config.properties.");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file", e); // Wrap as runtime exception
        }
        System.out.println("Loaded USERS_CSV_PATH: " + USERS_CSV_PATH);
        System.out.println("Loaded SUMMARY_CSV_PATH: " + SUMMARY_CSV_PATH);
        System.out.println("Loaded USER_DATA_BASE_DIR: " + USER_DATA_BASE_DIR);
        if (CSV_PATH != null) System.out.println("Loaded CSV_PATH: " + CSV_PATH);
    }
}