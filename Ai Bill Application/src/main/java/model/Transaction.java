package model;
//Transaction time, transaction type, transaction counterparty,
// commodity receipt/payment amount (yuan), payment method, current status,
// transaction order number, merchant order number, remarks
public class Transaction {
    private String transactionTime;
    private String transactionType;
    private String counterparty;
    private String commodity;
    private String inOut;
    private double paymentAmount;
    private String paymentMethod;
    private String currentStatus;
    private String orderNumber;
    private String merchantNumber;
    private String remarks;

    public Transaction() {
    }

    public Transaction(String transactionTime, String transactionType, String counterparty, String commodity, String inOut, double paymentAmount, String paymentMethod, String currentStatus, String orderNumber, String merchantNumber, String remarks) {
        this.transactionTime = transactionTime;
        this.transactionType = transactionType;
        this.counterparty = counterparty;
        this.commodity = commodity;
        this.inOut = inOut;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.currentStatus = currentStatus;
        this.orderNumber = orderNumber;
        this.merchantNumber = merchantNumber;
        this.remarks = remarks;
    }

    public String getInOut() {
        return inOut;
    }

    public void setInOut(String inOut) {
        this.inOut = inOut;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public void setPaymentAmount(double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public void setOrderNumber(String orderNumbe) {
        this.orderNumber = orderNumbe;
    }

    public void setMerchantNumber(String merchantNumber) {
        this.merchantNumber = merchantNumber;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public String getCommodity() {
        return commodity;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public String getRemarks() {
        return remarks;
    }
}
