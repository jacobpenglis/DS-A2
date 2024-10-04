import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/*
        Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24

        ContentServer.java - a java class that utilises sockets to send PUT requests

        Running ContentServer.java, it will read from the command line, both the 'server', 'port'
        and a filepath. The program will read the data from the .txt file specified by the filepath. Using the
        build Json parser, it will serialise the .txt data into .json format.

        Using the parsed socket and port, the program will attempt to open a socket and sent a 'GET' request. Directly 
        following, it will send the appropriate data headers, followed by the data itself. The program will close if it
        receives confirmation (201/200) from the Aggregation Server that it has received the data. Otherwise (400), it 
        will attempt to re-send the data 3 times before closing. 
 */

 public class ContentServer {
    private static String server;
    private static String filepath;
    private static int port;
    private static final int noRetries = 3;             // Maximum number of reconnection attempts
    private static final int retryDelay = 10000;        // Delay between retries, 10000ms
    private static final int socketDelay = 20000;       // Socket timeout timer, 20000ms
    private static final AtomicInteger lamportClock = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        ContentServer.filepath = args[1];
        if (args.length != 2) {
            System.out.println("Usage: java ContentServer <http://server:port> <filepath>");
            return;
        }
        
        try {
            String url = args[0];

            if (!url.startsWith("http://")) {
                throw new IllegalArgumentException("Usage: java ContentServer <http://server:port> <filepath>");
            }

            // Remove "http://"
            url = url.substring(7);

            // Split into server and port
            String[] parts = url.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Usage: java ContentServer <http://server:port> <filepath>");
            }

            ContentServer.server = parts[0];
            ContentServer.port = Integer.parseInt(parts[1]);

        } catch (Exception e) {
            System.out.println("400: Bad URL");
            System.out.println(e);
            return;
        }

        try {
            
            // Extract ID and store data proper,
            String[] data = extractID(ContentServer.filepath);
            String id = data[0];
            // Data is in txt format!
            String txtData = data[1];
            // Using build serialiser, turn into json format (to send)
            String jsonData = JsonSerialiser.serialiseToJson(txtData);
            // With data serialised, send a put request to the aggregation server
            sendPutRequest(id, jsonData);
        } catch (IOException e) {
            throw e;
        }
    }

    // Method extractID() - extracts ID and stores the rest of the data
    private static String[] extractID(String filePath) throws IOException {
        StringBuilder rawData = new StringBuilder();
        String id = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Iterate through data
            while ((line = reader.readLine()) != null) {
                // Check beginning for desired 'id:'
                if (line.startsWith("id:")) {
                    id = line.split(":")[1].trim();  // Extract the ID
                }
                // Append each line to raw data
                rawData.append(line).append("\n");
            }
        }

        // Handle no ID
        if (id == null) {
            throw new IOException("204: Data Missing");
        }

        // Return an array with extracted ID and the raw data
        return new String[]{id, rawData.toString()};
    }

    // Method sendPutRequest() - sends a PUT request to aggregation server, with data and ID
    private static void sendPutRequest(String id, String jsonData) {
        int retries = 0;            // Track number of reconnect retries
        boolean success = false;    // Track whether the connection is successful

        // Continuously attempt to send data, up to the max retries
        while (retries < noRetries && !success) {
            retries++;  

            try (Socket socket = new Socket(ContentServer.server, ContentServer.port)) {
                // Set socket timeout for detecting connection issues
                socket.setSoTimeout(socketDelay);
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send PUT request with specified headers
                out.println("PUT /weather.json HTTP/1.1");
                out.println("ID: " + id); // Custom header
                out.println("User-Agent: ATOMClient/1.0");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + jsonData.length());
                out.println();
                out.println(jsonData); // Send json data
                out.println();
                out.flush();

                System.out.println("Data serialised and sent!");

                // Read server response
                String response = in.readLine();
                if (response != null && (response.equals("200") || response.equals("201"))) {
                    if (response.startsWith("Lamport-Clock: ")){
                        int newLamport = Integer.parseInt(response.split(": ")[1]);
                        lamportClock.set(Math.max(lamportClock.get(), newLamport));
                        lamportClock.set(lamportClock.get() + 1);
                    }
                    System.out.println(response + ": OK");
                    success = true;  // Connection succeeded
                } else if (response.equals("204")) {
                    System.out.println("Error 204: Empty Json Content");
                } else if (response.equals("500")){
                    System.out.println("Error 500: Invalid Json Format");
                } else {
                    System.out.println("Error 400: Connection Failed");
                }
            } catch (SocketTimeoutException e) {
                // Handle socket timeouts
                System.out.println("Socket timeout! Attempting to reconnect...");
            } catch (SocketException e) {
                // Handle connection issues
                System.out.println("Connection failed! Attempting to reconnect...");
            } catch (IOException e) {
                System.out.println(ContentServer.filepath + ContentServer.server);
                System.out.println("IO Exception during connection.");
            }

            // Retry logic if failed
            if (!success && retries < noRetries) {
                System.out.println("Retrying connection... Attempt " + retries + " of " + noRetries);
                try {
                    Thread.sleep(retryDelay);  // Wait before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Retry interrupted.");
                }
            }
        }

        if (!success) {
            // If all retry attempts failed
            System.out.println("All reconnect attempts failed. Ending operation.");
        }
    }
}
