package DAO;

import model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CsvTransactionDao implements TransactionDao {
    //避免每次都调用loadFromCSV() 方法
    private List<Transaction> transactions;
    private boolean isLoad= false;
//HEAD 交易时间	交易类型	交易对方	商品	收/支	金额(元)	支付方式	当前状态	交易单号	商户单号	备注
    //
    @Override
    public List<Transaction> loadFromCSV(String filePath) throws IOException {
        try (
                Reader reader = new InputStreamReader(
                new BOMInputStream(Files.newInputStream(Paths.get(filePath))),
                StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()    //根据首行的内容设置为表格的头
                    .withIgnoreHeaderCase(false)  // 禁用忽略大小写
                    .withTrim(false);             // 禁用自动trim（防止意外空格问题）

            try (CSVParser csvParser = new CSVParser(reader, format)) {

                List<Transaction> transactions = new ArrayList<>();
                for (CSVRecord record : csvParser) {
                    Transaction transaction = parseRecord(record);
                    transactions.add(transaction);
                }
                reader.close();
                this.transactions = transactions;
                this.isLoad=true;
                return transactions;
            }
        }
    }


    private Transaction parseRecord(CSVRecord record) {
        return new Transaction(
                record.get("交易时间"),
                record.get("交易类型"),
                record.get("交易对方"),
                record.get("商品"),
                record.get("收/支"),
                Double.parseDouble(record.get("金额(元)").substring(1)),
                record.get("支付方式"),
                record.get("当前状态"),
                record.get("交易单号"),
                record.get("商户单号"),
                record.get("备注")
        );
    }

    // 根据交易单号删除交易记录
    public boolean deleteTransaction(String filePath, String orderNumber) throws IOException {
        if(isLoad==false){ loadFromCSV(filePath);}

        List<Transaction> updatedTransactions = transactions.stream()
                .filter(t -> !t.getOrderNumber().trim().equals(orderNumber))
                .collect(Collectors.toList());

        if (transactions.size() == updatedTransactions.size()) {
            return false;
        }

        writeTransactionsToCSV(filePath, updatedTransactions);
        return true;
    }

    // 添加交易
    public void addTransaction(String filePath, Transaction newTransaction) throws IOException {
        boolean fileExists = Files.exists(Paths.get(filePath)) && Files.size(Paths.get(filePath)) > 0;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             CSVPrinter csvPrinter = new CSVPrinter(writer, fileExists ? getCsvFormatWithoutHeader() : getCsvFormatWithHeader(filePath))) {

            csvPrinter.printRecord(
                    newTransaction.getTransactionTime(),
                    newTransaction.getTransactionType(),
                    newTransaction.getCounterparty(),
                    newTransaction.getCommodity(),
                    newTransaction.getInOut(),
                    "¥" + newTransaction.getPaymentAmount(),
                    newTransaction.getPaymentMethod(),
                    newTransaction.getCurrentStatus(),
                    newTransaction.getOrderNumber(),
                    newTransaction.getMerchantNumber(),
                    newTransaction.getRemarks()
            );

            csvPrinter.flush();
        }
    }

    // 统一写回 CSV
    public void writeTransactionsToCSV(String filePath, List<Transaction> transactions) throws IOException {
        // 1. 创建临时文件（确保与原文件同目录）
        File targetFile = new File(filePath);
        File tempFile = File.createTempFile("transaction_temp", ".csv", targetFile.getParentFile());

        // 2. 写入数据到临时文件（使用 try-with-resources 确保资源释放）
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath());
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"))) {

            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionTime(),
                        t.getTransactionType(),
                        t.getCounterparty(),
                        t.getCommodity(),
                        t.getInOut(),
                        "¥" + t.getPaymentAmount(),
                        t.getPaymentMethod(),
                        t.getCurrentStatus(),
                        t.getOrderNumber(),
                        t.getMerchantNumber(),
                        t.getRemarks()
                );
            }
            csvPrinter.flush(); // 强制刷盘
        } catch (IOException e) {
            tempFile.delete(); // 失败时删除临时文件
            throw e;
        }

        // 3. 原子性替换原文件
        Path source = tempFile.toPath();
        Path target = Paths.get(filePath);
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("移动失败原因: " + e.getMessage());
        }
    }

//    // 统一写回 CSV
//    public void writeTransactionsToCSV(String filePath, List<Transaction> transactions) throws IOException {
//        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
//             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
//                     "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注")))  {
//
//            for (Transaction t : transactions) {
//                csvPrinter.printRecord(
//                        t.getTransactionTime(),
//                        t.getTransactionType(),
//                        t.getCounterparty(),
//                        t.getCommodity(),
//                        t.getInOut(),
//                        "¥" + t.getPaymentAmount(),
//                        t.getPaymentMethod(),
//                        t.getCurrentStatus(),
//                        t.getOrderNumber(),
//                        t.getMerchantNumber(),
//                        t.getRemarks()
//                );
//            }
//        }
//    }

    // 获取 CSV 格式（包含表头）
    private CSVFormat getCsvFormatWithHeader(String filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                // 如果文件为空或不存在，返回默认表头
                return CSVFormat.DEFAULT.withHeader(
                        "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"
                );
            }
            return CSVFormat.DEFAULT.withHeader(headerLine.split(",")).withTrim();
        }
    }

    // 获取 CSV 格式（不包含表头）
    private CSVFormat getCsvFormatWithoutHeader() {
        return CSVFormat.DEFAULT.withTrim();
    }

    public boolean changeInformation(String orderNumber, String head, String value,String path) throws IOException{
        if(isLoad==false){ loadFromCSV(path);}
        for (int i = 0; i < transactions.size(); i++) {
            if(transactions.get(i).getOrderNumber().trim().equals(orderNumber)){
                Transaction newTransaction=transactions.get(i);
                switch (head){
                    case "transactionTime" :{
                        newTransaction.setTransactionTime(value);
                        break;}
                    case "transactionType" :{
                        newTransaction.setTransactionType(value);
                        break;
                    }
                    case "counterparty" :{
                        newTransaction.setCounterparty(value);
                        break;
                    } case "commodity" :{
                        newTransaction.setCommodity(value);
                        break;
                    } case "inOut" :{
                        newTransaction.setInOut(value);
                        break;
                    } case "paymentAmount" :{
                        newTransaction.setPaymentAmount(Double.parseDouble(value));
                        break;
                    } case "paymentMethod" :{
                        newTransaction.setPaymentMethod(value);
                        break;
                    } case "currentStatus" :{
                        newTransaction.setCurrentStatus(value);
                        break;
                    } case "orderNumber" :{
                        newTransaction.setOrderNumber(value);
                        break;
                    } case "merchantNumber" :{
                        newTransaction.setMerchantNumber(value);
                        break;
                    } case "remarks" :{
                        newTransaction.setRemarks(value);
                        break;
                    } default:{
                        System.err.println("error head");
                    }
                }

                boolean flag=deleteTransaction(path,transactions.get(i).getOrderNumber());
                if(flag) {
                    addTransaction(path,newTransaction);
                    System.out.println("success to delete");
                }
                else System.err.println("failed to delete");
            }
        }
        return true;
    }

    @Override
    public List<Transaction> getAllTransactions() throws IOException {
        return List.of();
    }

    @Override
    public void addTransaction(Transaction transaction) throws IOException {

    }

    @Override
    public boolean deleteTransaction(String orderNumber) throws IOException {
        return false;
    }

    @Override
    public boolean updateTransaction(String orderNumber, String fieldName, String newValue) throws IOException {
        return false;
    }

    @Override
    public Transaction getTransactionByOrderNumber(String orderNumber) throws IOException {
        return null;
    }
}
