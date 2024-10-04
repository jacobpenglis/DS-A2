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

// Unit Test: Testing empty message returns a '204' error signal

public class testInvalidPut02 {

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
    // testInvalidPut() - Makes a put request with invalid data
    public void testInvalidPut() throws IOException {
        // Assert that an IOException is thrown when invalid data is provided
        Exception exception = assertThrows(IOException.class, () -> {
            String[] args = {"http://localhost:4567", "./data/empty.txt"};
            ContentServer.main(args);
        });

        String expectedMessage = "204: Data Missing";
        String actualMessage = exception.getMessage();

        // Assert that the expected message is part of the actual message
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
