//package Service;
//
//import Service.AIservice.ColledgeStudentThread;
//import Service.AIservice.CollegeStudentNeeds;
//import org.junit.jupiter.api.*;
//
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Arrays;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//// No @ExtendWith needed as we are not using Mockito extensions
//class CollegeStudentNeedsTest {
//
//
//
//    private CollegeStudentNeeds collegeStudentNeeds=new CollegeStudentNeeds();
//    private static final String TEST_CSV_FILENAME = "src/test/resources/001.csv";
//    @Test
//    public void testGenerateBudget() throws IOException, InterruptedException {
//        Thread t=new Thread( new ColledgeStudentThread(TEST_CSV_FILENAME));
//        t.start();
//        t.join();
////        collegeStudentNeeds.generateBudget(TEST_CSV_FILENAME);
//    }
//    @Test
//    public void testParseStringToDouble(){
//        String s="asdas[369.39,1090.1]das";
//        double[] arr=collegeStudentNeeds.parseDoubleArrayFromString(s);
//        System.out.println(arr[0]+" "+arr[1]);
//    }
//
//
//}