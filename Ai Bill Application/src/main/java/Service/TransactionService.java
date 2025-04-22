package Service;

import model.Transaction;

import java.io.IOException;
import java.util.List;

public interface TransactionService {

    /**
     * 新增交易
     * @param transaction
     */
    void addTransaction(Transaction transaction) throws IOException;

    /**
     * 修改交易
     * @param transaction
     */
    void changeTransaction(Transaction transaction) throws Exception;

    /**
     * 根据订单号删除交易
     * @param orderNumber
     */
    boolean deleteTransaction(String orderNumber) throws Exception;

    /**
     * 根据用户输入信息查询交易
     * @param transaction
     * @return
     */
    List<Transaction> searchTransaction(Transaction transaction);

}
