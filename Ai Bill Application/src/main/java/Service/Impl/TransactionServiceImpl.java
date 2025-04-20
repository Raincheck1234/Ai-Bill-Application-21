package Service.Impl;

import DAO.CsvTransactionDao;
import Service.TransactionService;
import model.Transaction;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionServiceImpl implements TransactionService {
    private static final String CSV_PATH = "Ai Bill Application/src/main/resources/CSVForm/0001.csv";
    public static CsvTransactionDao csvTransactionDao;

    // 通过构造函数注入 DAO 初始化建议
    // CsvTransactionDao csvTransactionDao = new CsvTransactionDao();
    //TransactionService transactionService = new TransactionServiceImpl(csvTransactionDao);
    public TransactionServiceImpl(CsvTransactionDao csvTransactionDao) {
        this.csvTransactionDao = csvTransactionDao;
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
        csvTransactionDao.addTransaction(CSV_PATH, transaction);
    }

    @Override
    public void changeTransaction(Transaction updatedTransaction) throws IOException {
        // 1. 加载所有交易记录
        List<Transaction> allTransactions = csvTransactionDao.loadFromCSV(CSV_PATH);

        // 2. 查找并修改目标交易
        boolean found = false;
        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            String orderNumber=t.getOrderNumber();
            int length=orderNumber.length();
            orderNumber=orderNumber.substring(0,length-1);
            // 根据交易单号匹配记录（唯一标识）
            if (orderNumber.equals(updatedTransaction.getOrderNumber())) {
                // 3. 更新非空字段
                updateTransactionFields(t, updatedTransaction);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("未找到交易单号: " + updatedTransaction.getOrderNumber());
        }


        csvTransactionDao.writeTransactionsToCSV(CSV_PATH, allTransactions);
    }

    // 辅助方法：更新非空字段
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
    public boolean deleteTransaction(String orderNumber) throws IOException {
        boolean result = csvTransactionDao.deleteTransaction(CSV_PATH, orderNumber);
        if (!result) {
            System.err.println("未找到交易单号: " + orderNumber); // 或记录日志
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
            // 1. 读取所有交易记录（假设 filePath 已在类中定义或通过其他方式获取）
            List<Transaction> allTransactions = csvTransactionDao.loadFromCSV(CSV_PATH);

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
        }
    }



    // 判断单个交易记录是否匹配条件
    private boolean matchesCriteria(Transaction transaction, Transaction criteria) {
        return (criteria.getTransactionTime() == null || containsIgnoreCase(transaction.getTransactionTime(), criteria.getTransactionTime()))
                && (criteria.getTransactionType() == null || containsIgnoreCase(transaction.getTransactionType(), criteria.getTransactionType()))
                && (criteria.getCounterparty() == null || containsIgnoreCase(transaction.getCounterparty(), criteria.getCounterparty()))
                && (criteria.getCommodity() == null || containsIgnoreCase(transaction.getCommodity(), criteria.getCommodity()))
                && (criteria.getInOut() == null || containsIgnoreCase(transaction.getInOut(), criteria.getInOut()))
                && (criteria.getPaymentMethod() == null || containsIgnoreCase(transaction.getPaymentMethod(), criteria.getPaymentMethod()));
    }

    // 字符串模糊匹配（空条件视为匹配）
    private boolean containsIgnoreCase(String source, String target) {
        if (target == null || target.trim().isEmpty()) return true; // 空条件不参与筛选
        if (source == null) return false;
        return source.toLowerCase().contains(target.toLowerCase().trim());
    }

    // 安全的时间比较（处理 null 值）
    private int compareTransactionTime(String time1, String time2) {
        if (time1 == null && time2 == null) return 0;
        if (time1 == null) return -1;
        if (time2 == null) return 1;
        return time2.compareTo(time1); // 倒序排序
    }

}
