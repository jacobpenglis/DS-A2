import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/*
        Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24

        ContentServer.java - a java class that utilises sockets to send appropriate data; loaded from a local file and
        serialised to json.

        ContentServer then maintains that connection, and retries the connection in the case of an internal
        server error.
 */


public class ContentServer {
    // Initialise variables
    private static final String server = "localhost";
    private static final int port = 4567;
    private static final int noRetries = 3;  // Maximum number of reconnection attempts
    private static final int retryDelay = 5000;  // Delay between retries, 5000ms
    private static final int socketDelay = 20000;  // Socket timeout timer, 20000ms

    public static void main(String[] args) {
        // local filepath
        String filePath = "/Users/jacobpenglis/Desktop/Year 3/DS/DS-A2/src/weather_data.txt";
        try {
            // Specifically extract ID and store data proper,
            String[] data = extractID(filePath);
            String id = data[0];
            // Data is in txt format!
            String txtData = data[1];
            // using build serialiser, turn into json format (to send)
            String jsonData = JsonSerialiser.serialiseToJson(txtData);
            // with data serialised, send a put request to the aggregation server
            sendPutRequest(id, jsonData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method extractID() - extracts ID and stores the rest of the data
    private static String[] extractID(String filePath) throws IOException {
        StringBuilder rawData = new StringBuilder();
        String id = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Iterate through data
            while ((line = reader.readLine()) != null) {
                // Check beginning for desired 'id:'
                if (line.startsWith("id:")) {
                    // Value after ':', so extract [1]
                    id = line.split(":")[1].trim();  // Extract the ID
                }
                // Append each line to raw data
                rawData.append(line).append("\n");
            }
        }

        // Handle no ID
        if (id == null) {
            throw new IOException("ID not found in the data");
        }

        // Return an array with extracted ID and the raw data
        return new String[]{id, rawData.toString()};
    }

    // Method sendPutRequest() - sends a PUT request to aggregation server, with data and ID
    private static void sendPutRequest(String id, String jsonData) {
        int retries = 0;            // Track number of reconnect retries
        boolean success = false;    // Track whether the connection is successful

        // Continuously attempt to send data, given below number of retries
        while (retries < noRetries && !success) {
            // Increment
            retries++;
            // Create a new socket connection
            try (Socket socket = new Socket(server, port)) {
                // Set socket timeout for detecting connection issues (hope you're not lagging)
                socket.setSoTimeout(socketDelay);
                //
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send PUT request with specified headers
                out.println("PUT /weather.json HTTP/1.1");
                out.println("ID: " + id); // Custom header for my sanity
                out.println("User-Agent: ATOMClient/1/0");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + jsonData.length());
                out.println();
                out.println(jsonData); // send json data
                out.println(); // Imperative, content must finish with a newline character
                out.flush();

                System.out.println("Data serialised and sent!");
                retries = 1;

                // Actively monitor and (attempt to) maintain connection status
                maintainConnection(socket, out, in);

                success = true;  // Connection maintained without issues
            } catch (SocketTimeoutException e) {
                // Handle socket timeouts
                System.out.println("Socket timeout! Attempting to reconnect...");
            } catch (SocketException e) {
                // Handle connection issues (common)
                System.out.println("Connection failed! Attempting to reconnect...");
            } catch (IOException e) {
                System.out.println("");
            }

            // If the attempt has failed, but retries are left...
            if (!success && retries < noRetries) {
                System.out.println("Retrying connection... Attempt " + retries + " of " + noRetries);
                // Retry again, after waiting
                try {
                    Thread.sleep(retryDelay);  // Wait before retrying
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!success) {
            // No of reconnect retries has run out, :( - can be modified
            System.out.println("All reconnect attempts failed. Ending operation.");
        }
    }

    // Method maintainConnection() - Actively maintains connection with AggregationServer.java
    private static void maintainConnection(Socket socket, PrintWriter out, BufferedReader in) throws IOException {
        try {
            // Constantly check socket is open
            while (!socket.isClosed()) {
                // Constantly read in responses
                String response = in.readLine();
                if (response == null) {
                    throw new SocketException("Server closed connection");
                }

                // Socket opened, responses detected, maintain connection
                System.out.println("Connection established. Maintaining...");
                // Delay
                Thread.sleep(10000);
                // Send a heartbeat signal every ~10s to AggregationServer (can be modified)
                out.println("Connection Confirmed.");
                out.flush();
            }
        } catch (SocketException e) {
            // Handle Socket time out
            System.out.println("Connection lost, retrying...");
            throw e;
        } catch (InterruptedException e) {
            // Handle interrupts also
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted", e);
        }
    }
}
