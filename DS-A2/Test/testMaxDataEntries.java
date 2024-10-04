import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

// Unit Test; More then 20 entries

public class testMaxDataEntries {

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
    }

    @After
    public void tearDown() throws IOException {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        System.setOut(originalOut);
    }

    @Test
    public void testMaxEntriesLimit() throws IOException {
        // Insert more than 20 entries
        for (int i = 0; i < 21; i++) {
            String[] args = {"http://localhost:4567", "./data/weather_data1.txt"};
            ContentServer.main(args);
        }

        String[] getOldestArgs = {"http://localhost:4567", "ID8374849"}; 
        GETClient.main(getOldestArgs);

        assertTrue(outContent.toString().contains("ID8374849")); // Empty JSON response for replaced data

    }
}
