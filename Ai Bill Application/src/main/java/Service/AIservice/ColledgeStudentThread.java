package Service.AIservice;

// Add required imports
import Service.TransactionService; // Keep import if generateBudget throws Exception
// import java.io.IOException; // Removed specific IOException import if using general Exception


public class ColledgeStudentThread implements Runnable{
    // Remove the old budgetRange field, it's not used here
    // public String budgetRange;

    // Field to hold the injected CollegeStudentNeeds service instance
    private final CollegeStudentNeeds collegeStudentNeeds;
    // The file path is still needed to pass to the generateBudget method
    private final String filePath;


    /**
     * Constructor now accepts the CollegeStudentNeeds service instance and the file path.
     * The injected service instance already contains the necessary TransactionService.
     *
     * @param collegeStudentNeeds The CollegeStudentNeeds service instance to use for generating the budget.
     * @param filePath The path to the user's transaction data file.
     */
    public ColledgeStudentThread(CollegeStudentNeeds collegeStudentNeeds, String filePath) {
        this.collegeStudentNeeds = collegeStudentNeeds;
        this.filePath = filePath;
        System.out.println("ColledgeStudentThread initialized for file: " + filePath);
    }

    @Override
    public void run(){
        System.out.println("ColledgeStudentThread started for file: " + filePath);
        try {
            // Call the generateBudget method on the injected service instance
            double[] budget = collegeStudentNeeds.generateBudget(filePath);

            // Print the result or handle it as needed by the test/caller that starts this thread
            if (budget != null && budget.length == 2 && budget[0] != -1) {
                System.out.println("Budget generated in thread for file " + filePath + ": [" + budget[0] + ", " + budget[1] + "]");
            } else {
                // handle the case where budget generation failed or returned -1
                System.out.println("Budget generation in thread finished, but no valid budget was returned for file " + filePath + ".");
                // You might want to distinguish between "no data" (-1,-1) and other exceptions here.
            }
        } catch (Exception e) { // Catch Exception as generateBudget throws Exception
            System.err.println("Error generating budget in thread for file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("ColledgeStudentThread finished for file: " + filePath);
    }
}