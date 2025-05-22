package Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置常量类（线程安全初始化）
 */
public final class ConfigConstants {
    // 私有构造防止实例化
    private ConfigConstants() {}

    /**
     * transactionCSV路径加载逻辑
     */
    // CSV路径常量
    public static final String CSV_PATH; // Keep the original for now, will be dynamic per user later
    public static final String USERS_CSV_PATH; // 新增用户CSV路径

    // 静态初始化块（类加载时执行）
    static {
        Properties prop = new Properties();
        try (InputStream input = ConfigConstants.class.getClassLoader()
                .getResourceAsStream("config.properties")) { // 注意：这里的路径是相对于classpath的根目录

            if (input == null) {
                throw new RuntimeException("配置文件 config.properties 未找到在 classpath 中");
            }

            prop.load(input);
            // 从 properties 读取值
            // 注意：从 properties 读取的路径可能需要调整，使其在文件系统中可访问
            CSV_PATH = prop.getProperty("csv.path"); // Might still be used in existing places
            USERS_CSV_PATH = prop.getProperty("csv.users_path"); // 读取用户CSV路径

            // Basic validation
            if (CSV_PATH == null || CSV_PATH.trim().isEmpty()) {
                System.err.println("Warning: 'csv.path' not found or is empty in config.properties.");
                // Depending on whether this is strictly required initially, might throw error
            }
            if (USERS_CSV_PATH == null || USERS_CSV_PATH.trim().isEmpty()) {
                throw new RuntimeException("'csv.users_path' not found or is empty in config.properties.");
            }

        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e); // 转换为运行时异常
        }
        System.out.println("Loaded CSV_PATH: " + CSV_PATH);
        System.out.println("Loaded USERS_CSV_PATH: " + USERS_CSV_PATH);
    }
}