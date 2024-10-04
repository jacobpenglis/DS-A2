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

// Unit Test: Tests server fault-tolerance, by testing if it retains data after being reset

public class testServerStartup {

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
    // testValidGETRequest() - Makes a get request with given arguments
    public void testValidGetRequest() throws IOException, InterruptedException {
        // Insert data first
        String[] args = {"http://localhost:4567", "./data/weather_data.txt"};
        ContentServer.main(args);
        assertTrue(outContent.toString().contains("201"));

        tearDown();
        setUp();

        String[] args1 = {"http://localhost:4567", "IDS60901"};
        GETClient.main(args1);
        assertTrue(outContent.toString().contains("IDS60901"));

    }
}
