import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Integration Test: Testing multiple GET Client requests

public class testConcurrency02 {

    private Thread serverThread;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUp() throws IOException, InterruptedException {
        serverThread = new Thread(() -> {
            AggregationServer.main(null);
        });
        serverThread.start();

        Thread.sleep(1000);
        System.setOut(new PrintStream(outContent));

        String[] putArgs = {"http://localhost:4567", "./data/weather_data.txt"};
        ContentServer.main(putArgs);
        assertTrue(outContent.toString().contains("201"));
    }

    @After
    public void tearDown() throws IOException {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        System.setOut(originalOut);
    }

    @Test
    // testForGetConcurrency() - Initialises multiple clients and checks concurrency with GET requests
    public void testForGetConcurrency() throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);  // Creating a pool of 10 threads

        Runnable clientTask = () -> {
            String[] getArgs = {"http://localhost:4567", "IDS60901"};
            try {
                GETClient.main(getArgs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Run simultaneously
        for (int i = 0; i < 10; i++) {
            executor.submit(clientTask);  
        }

        executor.shutdown();

        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        // Check if the GET requests succeeded
        assertTrue(outContent.toString().contains("IDS60901")); 
    }
}
