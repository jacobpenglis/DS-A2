import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Integration Test: Testing multiple Content Servers concurrently

public class testConcurrency {

    private Thread serverThread;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    // setUp() runs before Test - opens thread, waits for one second, redirects stdout
    public void setUp() throws IOException, InterruptedException {

        // Start AggregationServer in a new thread
        serverThread = new Thread(() -> {
            AggregationServer.main(null);
        });
        serverThread.start();

        Thread.sleep(1000);
        System.setOut(new PrintStream(outContent));
    }

    @After
    // tearDown() runs after Test - Closes thread, clears backup txt and redirects stdout.
    public void tearDown() throws IOException {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        File backupFile = new File("backup.txt");
        if (backupFile.exists()) {
            PrintWriter writer = new PrintWriter(new FileWriter(backupFile, false));
            writer.print(""); 
            writer.close();
        }
        
        System.setOut(originalOut); 
    }

    @Test
    public void testForConcurrency() throws IOException, InterruptedException {
        // Create a pool of 10 threads
        ExecutorService executor = Executors.newFixedThreadPool(10);  

        // Simulate multiple content servers making concurrent PUT requests
        Runnable clientTask = () -> {
            String[] args = {"http://localhost:4567", "./data/weather_data.txt"};
            try {
                ContentServer.main(args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < 10; i++) {
            executor.submit(clientTask);  
        }

        executor.shutdown();

        // Wait for all tasks to complete
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        // Check if the PUT requests succeeded
        assertTrue(outContent.toString().contains("201") || outContent.toString().contains("200"));
        assertTrue(outContent.toString().contains("Data Received")); 
    }
}
