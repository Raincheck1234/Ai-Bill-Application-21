package Service.Impl;

import Constants.ConfigConstants;
import DAO.CsvTransactionDao;
import Service.TransactionService;
import Utils.CacheUtil;
import model.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TransactionServiceImpl implements TransactionService {
    public static CsvTransactionDao csvTransactionDao;
    private static final String CAFFEINE_KEY = "transactions";

    /**
     * 定义缓存：键为固定值（因为只有一个CSV文件），值为交易列表
     */
    private final CacheUtil<String, List<Transaction>, Exception> cache;

    /**
     * 通过构造函数注入路径和csvTransactionDao
     * @param csvTransactionDao
     * @throws IOException
     */
    public TransactionServiceImpl(CsvTransactionDao csvTransactionDao)  {
        // 1. 注入Dao层接口
        TransactionServiceImpl.csvTransactionDao = csvTransactionDao;
        // 2. 初始化CacheUtil
        this.cache = new CacheUtil<String, List<Transaction>, Exception>(
                key -> {
                    try {
                        return csvTransactionDao.loadFromCSV(ConfigConstants.CSV_PATH);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, // 定义缓存未命中的执行逻辑
                1, 10, 1
        );
    }

    /**
     * 从transactions缓存中读取transactions如果没有则从csv文件中找
     * @return
     * @throws IOException
     */
    private List<Transaction> getAllTransactions() throws Exception {
        return cache.get(CAFFEINE_KEY);
    }


    /**
     * 新增交易
     * @param transaction
     */
    @Override
    public void addTransaction(Transaction transaction) throws IOException {
        // 设置交易时间为当前时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String currentTime = now.format(formatter);
        transaction.setTransactionTime(currentTime);

        // 调用 DAO 层方法添加交易
        csvTransactionDao.addTransaction(ConfigConstants.CSV_PATH, transaction);
    }

    @Override
    public void changeTransaction(Transaction updatedTransaction) throws Exception {
        // 1. 加载所有交易记录 先加载 caffeine存储的缓存
        List<Transaction> allTransactions = getAllTransactions();

        // 2. 查找并修改目标交易
        boolean found = false;
        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            // 根据交易单号匹配记录（唯一标识）
            if (t.getOrderNumber().equals(updatedTransaction.getOrderNumber())) {
                // 3. 更新非空字段
                updateTransactionFields(t, updatedTransaction);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("未找到交易单号: " + updatedTransaction.getOrderNumber());
        }
        // 使用事务将写后数据写回csv文件
        csvTransactionDao.writeTransactionsToCSV(ConfigConstants.CSV_PATH, allTransactions);
        // 修改后使缓存失效 （删缓存）
        cache.invalidate("transactions");
    }

    /**
     *  辅助方法：更新非空字段
     * @param target
     * @param source
     */
    private void updateTransactionFields(Transaction target, Transaction source) {
        if (source.getTransactionTime() != null && !source.getTransactionTime().isEmpty()) {
            target.setTransactionTime(source.getTransactionTime());
        }
        if (source.getTransactionType() != null && !source.getTransactionType().isEmpty()) {
            target.setTransactionType(source.getTransactionType());
        }
        if (source.getCounterparty() != null && !source.getCounterparty().isEmpty()) {
            target.setCounterparty(source.getCounterparty());
        }
        if (source.getCommodity() != null && !source.getCommodity().isEmpty()) {
            target.setCommodity(source.getCommodity());
        }
        if (source.getInOut() != null && !source.getInOut().isEmpty()) {
            target.setInOut(source.getInOut());
        }
        if (source.getPaymentAmount() != 0.0) { // 假设金额为0表示未修改
            target.setPaymentAmount(source.getPaymentAmount());
        }
        if (source.getPaymentMethod() != null && !source.getPaymentMethod().isEmpty()) {
            target.setPaymentMethod(source.getPaymentMethod());
        }
        if (source.getCurrentStatus() != null && !source.getCurrentStatus().isEmpty()) {
            target.setCurrentStatus(source.getCurrentStatus());
        }
        if (source.getMerchantNumber() != null && !source.getMerchantNumber().isEmpty()) {
            target.setMerchantNumber(source.getMerchantNumber());
        }
        if (source.getRemarks() != null && !source.getRemarks().isEmpty()) {
            target.setRemarks(source.getRemarks());
        }
    }

    /**
     * 根据订单号删除交易 (若成功则返回true)
     * @param orderNumber
     */
    @Override
    public boolean deleteTransaction(String orderNumber) throws Exception {
        boolean result = csvTransactionDao.deleteTransaction(ConfigConstants.CSV_PATH, orderNumber);
        if (!result) {
            throw new Exception("未找到交易单号: " + orderNumber); // 或记录日志
        }
        return result;
    }

    /**
     * 按照查询条件查询交易信息
     * @param searchCriteria
     * @return
     */
    @Override
    public List<Transaction> searchTransaction(Transaction searchCriteria) {
        try {
            // 1. 读取所有交易记录 首先读取caffeine
            List<Transaction> allTransactions = getAllTransactions();

            // 2. 动态模糊匹配
            List<Transaction> matched = new ArrayList<>();
            for (Transaction t : allTransactions) {
                if (matchesCriteria(t, searchCriteria)) {
                    matched.add(t);
                }
            }

            // 3. 按交易时间倒序排序
            matched.sort((t1, t2) -> compareTransactionTime(t2.getTransactionTime(), t1.getTransactionTime()));

            return matched;
        } catch (IOException e) {
            e.printStackTrace();
            return List.of(); // 实际应用中应处理异常
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 辅助方法:判断单个交易记录是否匹配条件
     * @param transaction
     * @param criteria
     * @return
     */
    private boolean matchesCriteria(Transaction transaction, Transaction criteria) {
        return (criteria.getTransactionTime() == null || containsIgnoreCase(transaction.getTransactionTime(), criteria.getTransactionTime()))
                && (criteria.getTransactionType() == null || containsIgnoreCase(transaction.getTransactionType(), criteria.getTransactionType()))
                && (criteria.getCounterparty() == null || containsIgnoreCase(transaction.getCounterparty(), criteria.getCounterparty()))
                && (criteria.getCommodity() == null || containsIgnoreCase(transaction.getCommodity(), criteria.getCommodity()))
                && (criteria.getInOut() == null || containsIgnoreCase(transaction.getInOut(), criteria.getInOut()))
                && (criteria.getPaymentMethod() == null || containsIgnoreCase(transaction.getPaymentMethod(), criteria.getPaymentMethod()));
    }

    /**
     * 辅助方法：字符串模糊匹配（空条件视为匹配）
     * @param source
     * @param target
     * @return
     */
    private boolean containsIgnoreCase(String source, String target) {
        if (target == null || target.trim().isEmpty()) return true; // 空条件不参与筛选
        if (source == null) return false;
        return source.toLowerCase().contains(target.toLowerCase().trim());
    }

    /**
     * 辅助方法：安全的时间比较（处理 null 值）
     * @param time1
     * @param time2
     * @return
     */
    private int compareTransactionTime(String time1, String time2) {
        if (time1 == null && time2 == null) return 0;
        if (time1 == null) return -1;
        if (time2 == null) return 1;
        return time2.compareTo(time1); // 倒序排序
    }

}
