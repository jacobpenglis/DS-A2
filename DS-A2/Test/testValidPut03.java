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

// Unit Test: Testing '200' returns after Aggregation Server has shut down and restarted 

public class testValidPut03 {

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
            writer.print(""); // Empty the file
            writer.close();
        }

        System.setOut(originalOut);

    }

    @Test
    // testValidPutRequest() - Makes a put request with valid data
    public void testValidPutRequest() throws IOException, InterruptedException {
        // Using Built ContentServer to send a valid PUT request
        String[] args = {"http://localhost:4567", "./data/weather_data.txt"};
        ContentServer.main(args);
        assertTrue(outContent.toString().contains("201"));

        tearDown();
        Thread.sleep(2000);
        setUp();

        String[] args2 = {"http://localhost:4567", "./data/weather_data.txt"};
        ContentServer.main(args2);
        assertTrue(outContent.toString().contains("200"));
    }
}
