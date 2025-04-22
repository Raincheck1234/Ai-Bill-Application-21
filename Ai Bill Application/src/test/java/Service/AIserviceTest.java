package Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import Service.AIservice.AITransactionService;

public class AIserviceTest {

    @Test
    public void testAnalyzeTransactions() {
        AITransactionService service = new AITransactionService();

        String userRequest = "Please help me analyze the income and expenses for this month";
        String filePath = "src/test/resources/sample_transactions.csv";

        String result = service.analyzeTransactions(userRequest, filePath, "2025/04/04", "");

        assertNotNull(result, "AI analysis result should not be null");
        System.out.println("AI analysis result: " + result);
    }

}
