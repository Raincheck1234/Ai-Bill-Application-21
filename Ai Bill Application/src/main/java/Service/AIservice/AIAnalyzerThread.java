package Service.AIservice;

// Add required imports
import Service.TransactionService; // Import if analyzeTransactions throws Exception


public class AIAnalyzerThread implements Runnable {
    private final String userRequest;
    private final String filePath; // Still needed for analyzeTransactions method signature
    private final String startTimeStr;
    private final String endTimeStr;

    // Field to hold the injected AITransactionService service instance
    private final AITransactionService aiTransactionService;


    /**
     * Constructor now accepts the AITransactionService service instance
     * and the parameters for the analysis request.
     *
     * @param aiTransactionService The AITransactionService service instance to use.
     * @param userRequest The user's natural language request.
     * @param filePath The path to the user's transaction data file.
     * @param startTimeStr The start time string for filtering.
     * @param endTimeStr The end time string for filtering.
     */
    public AIAnalyzerThread(AITransactionService aiTransactionService, String userRequest, String filePath, String startTimeStr, String endTimeStr) {
        this.aiTransactionService = aiTransactionService; // Inject the service
        this.userRequest = userRequest;
        this.filePath = filePath; // Still needed for analyzeTransactions call
        this.startTimeStr = startTimeStr;
        this.endTimeStr = endTimeStr;
        System.out.println("AIAnalyzerThread initialized for file: " + filePath);
    }

    @Override
    public void run() {
        System.out.println("AIAnalyzerThread started for analysis request: '" + userRequest + "' on file: " + filePath);
        try {
            // Call the analyzeTransactions method on the injected service instance
            String result = aiTransactionService.analyzeTransactions(userRequest, filePath, startTimeStr, endTimeStr);

            // Print the result or handle it as needed by the test/caller that starts this thread
            System.out.println("AI analysis result from thread: " + result);

            // TODO: If this thread is used by a UI program, use SwingUtilities.invokeLater() to update a UI component with 'result'
            // This indicates this class might be intended for background tasks separate from the main UI refresh cycle handled in MenuUI.
        } catch (Exception e) { // Catch Exception as analyzeTransactions throws Exception
            System.err.println("Error during AI analysis in thread for file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
            // You might want to pass this error back to the caller if it's a UI context.
        }
        System.out.println("AIAnalyzerThread finished for file: " + filePath);
    }
}