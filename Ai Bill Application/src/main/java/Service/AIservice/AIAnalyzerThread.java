package Service.AIservice;

public class AIAnalyzerThread implements Runnable {
    private final String userRequest;
    private final String filePath;
    private final String startTimeStr;
    private final String endTimeStr;

    public AIAnalyzerThread(String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        this.userRequest = userRequest;
        this.filePath = filePath;
        this.startTimeStr = startTimeStr;
        this.endTimeStr = endTimeStr;
    }

    @Override
    public void run() {
        String result = new AITransactionService().analyzeTransactions(userRequest, filePath, startTimeStr, endTimeStr);
        System.out.println("AI analysis result: " + result);
        // TODO: If it's a UI application, you can use SwingUtilities.invokeLater() to update UI components
    }
}
