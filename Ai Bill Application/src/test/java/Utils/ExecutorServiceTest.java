package Utils; // Put the test in a relevant package, e.g., Utils where CacheManager is

import com.group21.ai.Main; // Import the Main class to access the ExecutorService
import org.junit.jupiter.api.*; // Import JUnit 5 annotations

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean; // For checking side effects

import static org.junit.jupiter.api.Assertions.*; // Import assertions


// Test class for checking ExecutorService functionality
public class ExecutorServiceTest {

    // We will test the static ExecutorService created in Main.
    // Note: Accessing static state in tests can sometimes make tests less isolated.
    // A more robust approach might be to dependency inject ExecutorService everywhere and test with a mock/test ExecutorService instance.
    // But to test that Main sets up the global ExecutorService, accessing it statically is necessary here.

    @BeforeAll // This method runs once before all test methods in this class
    static void setUpAll() {
        // Ensure the Main class is initialized, which should create the ExecutorService
        // Running a minimal part of Main's initialization if the full Main.main is too complex
        // Or if Main.main is designed to run the GUI, maybe just call the part that initializes ExecutorService
        // For simplicity, if Main.main is the entry point that sets up everything, we might need to
        // structure it so initialization can be triggered without starting the full GUI.
        // Assuming Main.main() is called elsewhere (e.g., by your test runner setup if it exists)
        // If not, you might need a helper method in Main to just initialize the service:
        // public static void initializeExecutorService() { executorService = Executors.newFixedThreadPool(4); }
        // And call Main.initializeExecutorService() here.

        // Let's assume Main.main() is called by your test runner setup or you have a helper initializer.
        // If you run this test directly, and Main.main() hasn't run, getExecutorService() might be null.
        // A simple way to ensure it's not null for this test is to call Main.main()
        // or the initialization part, but this might start the GUI.
        // For now, let's assume getExecutorService() will return a non-null instance in your testing environment.

        // Alternative: Create a test ExecutorService just for this test class
        // ExecutorService testExecutor = Executors.newSingleThreadExecutor();
        // But the goal is to test the *application's* executor.
        System.out.println("ExecutorServiceTest: @BeforeAll - Setting up.");
        // You might need to ensure Main.main() has been called or equivalent setup.
        // This is a common challenge when testing applications with global static state or GUI start in main.
        // For demonstration, we'll rely on Main.getExecutorService() returning the actual service.
        ExecutorService service = Main.getExecutorService();
        assertNotNull(service, "ExecutorService in Main should be initialized.");
        assertFalse(service.isShutdown(), "ExecutorService should not be shut down initially.");

        System.out.println("ExecutorServiceTest: @BeforeAll - Setup complete.");
    }

    @AfterAll // This method runs once after all test methods in this class
    static void tearDownAll() {
        // No explicit shutdown needed here if we are testing the application's global service,
        // as the shutdown hook in Main should handle it when the JVM exits after tests.
        // If you were testing a separate test ExecutorService, you would shut it down here.
        System.out.println("ExecutorServiceTest: @AfterAll - Tearing down.");
    }


    @Test
    @DisplayName("Test ExecutorService is not null")
    void testExecutorServiceIsNotNull() {
        System.out.println("ExecutorServiceTest: Running testExecutorServiceIsNotNull...");
        ExecutorService service = Main.getExecutorService();
        // Assert that the service instance retrieved from Main is not null
        assertNotNull(service, "The ExecutorService instance retrieved from Main should not be null.");
        System.out.println("ExecutorServiceTest: testExecutorServiceIsNotNull finished.");
    }

    @Test
    @DisplayName("Test ExecutorService can execute a simple Runnable task")
    void testExecutorServiceCanExecuteRunnable() throws Exception {
        System.out.println("ExecutorServiceTest: Running testExecutorServiceCanExecuteRunnable...");
        ExecutorService service = Main.getExecutorService();
        assertNotNull(service, "ExecutorService should be initialized.");

        // Use an AtomicBoolean to check if the task was executed
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        // Create a simple Runnable task
        Runnable simpleTask = () -> {
            System.out.println("ExecutorServiceTest: Runnable task is running in background.");
            // Simulate some work
            try {
                Thread.sleep(100); // Sleep for 100 milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Set the flag to true when the task finishes
            taskExecuted.set(true);
            System.out.println("ExecutorServiceTest: Runnable task finished.");
        };

        // Submit the Runnable task to the ExecutorService
        Future<?> future = service.submit(simpleTask);

        System.out.println("ExecutorServiceTest: Task submitted. Waiting for completion...");

        // Wait for the task to complete (using Future.get() will block until completion)
        // Using a timeout is important in tests to prevent infinite waits
        future.get(5, TimeUnit.SECONDS); // Wait up to 5 seconds for the task to complete

        System.out.println("ExecutorServiceTest: Task completed.");

        // Assert that the task was executed (check the AtomicBoolean flag)
        assertTrue(taskExecuted.get(), "The Runnable task should have been executed by the ExecutorService.");
        System.out.println("ExecutorServiceTest: testExecutorServiceCanExecuteRunnable finished.");
    }

    @Test
    @DisplayName("Test ExecutorService can execute a simple Callable task and return result")
    void testExecutorServiceCanExecuteCallable() throws Exception {
        System.out.println("ExecutorServiceTest: Running testExecutorServiceCanExecuteCallable...");
        ExecutorService service = Main.getExecutorService();
        assertNotNull(service, "ExecutorService should be initialized.");

        // Create a simple Callable task that returns a value
        Callable<String> simpleCallable = () -> {
            System.out.println("ExecutorServiceTest: Callable task is running in background.");
            // Simulate some work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String result = "Callable Task Done!";
            System.out.println("ExecutorServiceTest: Callable task finished, returning: " + result);
            return result; // Return a result
        };

        // Submit the Callable task to the ExecutorService
        Future<String> future = service.submit(simpleCallable);

        System.out.println("ExecutorServiceTest: Callable task submitted. Waiting for result...");

        // Wait for the task to complete and get the result
        String result = future.get(5, TimeUnit.SECONDS); // Wait up to 5 seconds

        System.out.println("ExecutorServiceTest: Callable task completed. Received result: " + result);

        // Assert that the result is as expected
        assertNotNull(result, "The Callable task should return a non-null result.");
        assertEquals("Callable Task Done!", result, "The result from the Callable task should match the expected value.");
        System.out.println("ExecutorServiceTest: testExecutorServiceCanExecuteCallable finished.");
    }

    // You could add more tests, for example:
    // - Test submitting multiple tasks to see if the thread pool size limit is respected (harder to test directly).
    // - Test the shutdown process (requires controlling the ExecutorService lifecycle within the test, which is tricky if testing the global one).
}