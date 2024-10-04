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

// Unit Test: Tests server deleting data after 30s

public class testDataExpiry {

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
    // testExpiry() -  Makes a GET request and waits 30s for deletion
    public void testExpiry() throws IOException, InterruptedException {
        // Insert data
        String[] args = {"http://localhost:4567", "./data/weather_data.txt"};
        ContentServer.main(args);
        assertTrue(outContent.toString().contains("201"));

        // Wait for 30 seconds (data expiry time)
        Thread.sleep(32000);

        String[] getArgs = {"http://localhost:4567", "ID8374849"}; // Example ID
        GETClient.main(getArgs);

        // Expecting an empty response 
        assertTrue(outContent.toString().contains("{}"));
    }
}
