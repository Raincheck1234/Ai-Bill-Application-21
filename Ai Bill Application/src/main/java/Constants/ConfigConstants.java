package Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置常量类（线程安全初始化）
 */
public final class ConfigConstants {
    private ConfigConstants() {} // 私有构造防止实例化

    // CSV路径常量
    public static final String CSV_PATH; // Original, may still be referenced in old code
    public static final String USERS_CSV_PATH; // User CSV path
    public static final String SUMMARY_CSV_PATH; // 新增汇总统计CSV路径


    // 静态初始化块（类加载时执行）
    static {
        Properties prop = new Properties();
        try (InputStream input = ConfigConstants.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("配置文件 config.properties 未找到在 classpath 中");
            }

            prop.load(input);

            CSV_PATH = prop.getProperty("csv.path");
            USERS_CSV_PATH = prop.getProperty("csv.users_path");
            SUMMARY_CSV_PATH = prop.getProperty("csv.summary_path"); // 读取汇总统计CSV路径


            // Basic validation
            if (USERS_CSV_PATH == null || USERS_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.users_path' not found or is empty in config.properties.");
            }
            if (SUMMARY_CSV_PATH == null || SUMMARY_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.summary_path' not found or is empty in config.properties.");
            }

        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e); // 转换为运行时异常
        }
        System.out.println("Loaded USERS_CSV_PATH: " + USERS_CSV_PATH);
        System.out.println("Loaded SUMMARY_CSV_PATH: " + SUMMARY_CSV_PATH);
        if (CSV_PATH != null) System.out.println("Loaded CSV_PATH: " + CSV_PATH); // Optional print
    }
}