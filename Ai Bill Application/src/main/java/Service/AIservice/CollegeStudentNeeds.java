package Service.AIservice;

import Constants.ConfigConstants;
import DAO.CsvTransactionDao;
import Service.Impl.TransactionServiceImpl;
import Utils.CacheUtil;
import model.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CollegeStudentNeeds {
    private final String requestBudge="我是一名预算有限的大学生，请根据下面我给出的花费，帮助我给下周预算范围，必须以[最低预算，最高预算],的方式给出回答，不能有多余的回复。";
    private final String requestTips="我是一名预算有限的大学生，请给我推荐一些省钱方法。";

    private final String requestRecognition="下面我将给你一些账单的信息，请推测这个账单是什么方面的消费: ";
    CsvTransactionDao dao;

    /**
     * 使用Caffeine缓存CSV文件
     */
    private static final String CAFFEINE_KEY = "transactions";  // 存储的键
    /**
     * 定义缓存：键为固定值（因为只有一个CSV文件），值为交易列表
     */
    private final CacheUtil<String, List<Transaction>, Exception> cache;

    public CollegeStudentNeeds() {
        TransactionServiceImpl.csvTransactionDao = dao;
        this.cache = new CacheUtil<String, List<Transaction>, Exception>(
                key -> {
                    try {
                        return dao.loadFromCSV(ConfigConstants.CSV_PATH);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, // 定义缓存未命中的执行逻辑
                1, 10, 1
        );

    }
    public String RecognizeTransaction(Transaction transaction){
        StringBuilder sb=new StringBuilder();
        sb.append("交易类型:").append(transaction.getTransactionType()+",").append("交易对方").append(transaction.getCounterparty()+",")
                .append("商品:").append(transaction.getCommodity()+",").append("收/支:").append(transaction.getInOut()+",")
                        .append("金额(元):").append(transaction.getPaymentAmount()+",").append("支付方式").append(transaction.getPaymentMethod()+",").append("备注:").append(transaction.getRemarks());
        return  new AITransactionService().askAi(requestRecognition+sb.toString());
    }

    public String generateTipsForSaving(){
        return  new AITransactionService().askAi(requestTips);
    }
    //按周统计已有的支出，依靠ai得到下周的预算
    public double[] generateBudget(String path) throws IOException {
        List<Transaction> transactions=cache.get(CAFFEINE_KEY);

        int size=transactions.size();
        if(size==0||size==1) return new double[]{-1,-1};
        int count=1;
        int base=0;
        int index=0;//指示首个适合当基准的时间点
        for (index = 0; index < size; index++) {
            int data=convertDateToNumber(transactions.get(index).getTransactionTime().split(" ")[0]);
            if(data>0){
                base=data;
                break;
            }
        }
        List<Double> list=new ArrayList<>();
        double weekConsumption=0;
        for (int i = 0; i < size; i++) {
            int data=convertDateToNumber(transactions.get(i).getTransactionTime().split(" ")[0]);
            if(data<0) continue;
            if((base-data>=0&&base-data<7)&&transactions.get(i).getInOut().equals("支出")){
                weekConsumption+=transactions.get(i).getPaymentAmount();
            }else if(base-data>=7){
                base=data;
                list.add(weekConsumption);
                count++;
                weekConsumption=0;
                if(transactions.get(i).getInOut().equals("支出")){
                    weekConsumption+=transactions.get(i).getPaymentAmount();
                }
            }
        }
        StringBuilder stringBuilder=new StringBuilder();
        for (int i = 0; i < count-1; i++) {
            stringBuilder.append("第");
            stringBuilder.append(i);
            stringBuilder.append("周:花费");
            stringBuilder.append(list.get(i));
            stringBuilder.append("元;");
        }
        String answer=new AITransactionService().askAi(requestBudge+stringBuilder.toString());
        System.out.println(answer);
        double[] ret=new double[2];
        ret=parseDoubleArrayFromString(answer);
        return ret;
    }

    private int convertDateToNumber(String date) {
        // format yyyy/MM/dd
        String[] parts = date.split("/");
        if (parts.length != 3) {
            return -1; // Invalid date format
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            int days = day;
            int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}; // Days in each month
            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                daysInMonth[2] = 29; // February has 29 days in a leap year
            }
            for (int i = 1; i < month; i++) {
                days += daysInMonth[i];
            }
            return year * 365 + (year / 4 - year / 100 + year / 400) + days;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    public double[] parseDoubleArrayFromString(String input) {
        int length=input.length();
        int l=0,rightindex=input.length();
        // 1. 去除首尾括号
        for (int i = 0; i < length; i++) {
            if(input.charAt(i)<58&&input.charAt(i)>47){
                l=i;
                break;
            }
        }
        for (int r =length-1 ; r >0; r--) {
            if(input.charAt(r)<58&&input.charAt(r)>47){
                rightindex=r;
                break;
            }
        }
        String content = input.substring(l, rightindex+1 ); // 获取 "1.1, 11.0" 或 " 2.5 ,3.14 , -0.5 "

        // 2. 按逗号分割
        String[] numberStrings1 = content.split("，"); // 得到 ["1.1", " 11.0"] 或 [" 2.5 ", "3.14 ", " -0.5 "]
        String[] numberStrings2 = content.split(","); // 得到 ["1.1", " 11.0"] 或 [" 2.5 ", "3.14 ", " -0.5 "]
        String[] numberStrings= numberStrings1.length> numberStrings2.length?numberStrings1:numberStrings2;
//        System.out.println(numberStrings[0]);
//        System.out.println(numberStrings[1]);
        // 3. & 4. 遍历、清理、解析并存储
        List<Double> numberList = new ArrayList<>();
        try {
            for (String numStr : numberStrings) {
                String trimmedStr = numStr.trim(); // 去除首尾空格
                if (!trimmedStr.isEmpty()) { // 避免分割后产生空字符串（例如 "[1.1, , 2.2]"）
                    numberList.add(Double.parseDouble(trimmedStr)); // 解析为 double
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("错误：字符串无法解析为 double 类型。");
            return null; // 或者抛出异常
        }

        // 将 List<Double> 转换为 double[]
        double[] result = new double[numberList.size()];
        for (int i = 0; i < numberList.size(); i++) {
            result[i] = numberList.get(i);
        }
        return result;
    }
}
