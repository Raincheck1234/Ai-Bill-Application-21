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
    public static final String CSV_PATH;
    // 静态初始化块（类加载时执行）
    static {
        Properties prop = new Properties();
        try (InputStream input = ConfigConstants.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            prop.load(input);
            CSV_PATH = prop.getProperty("csv.path");

        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e); // 转换为运行时异常
        }
    }
}