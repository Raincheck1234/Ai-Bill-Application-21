package Service;

import Service.AIservice.AIAnalyzerThread;
import org.junit.jupiter.api.Test;

public class AIAnalyzerThreadTest {

    @Test
    public void testRunAIAnalyzerThread() throws InterruptedException {
        String userRequest = "Please help me analyze the recent transaction income and expenses";
        String filePath = "src/test/resources/sample_transactions.csv";
        String startTimeStr = "2025/04/04";
        String endTimeStr = "";

        // Start the thread
        Thread thread = new Thread(new AIAnalyzerThread(userRequest, filePath, startTimeStr, endTimeStr));
        thread.start();

        // Wait for the thread to complete execution
        thread.join();
    }
}

