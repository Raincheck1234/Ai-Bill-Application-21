package Service.AIservice;

import java.io.IOException;

public class ColledgeStudentThread implements Runnable{
//    private final int userRequest;
    private final String filePath;
    public String budgetRange;
    public ColledgeStudentThread( String filePath) {
//        this.userRequest = userRequest;
        this.filePath = filePath;
    }

    @Override
    public void run(){
        CollegeStudentNeeds collegeStudentNeeds=new CollegeStudentNeeds();
        try {
            collegeStudentNeeds.generateBudget(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
