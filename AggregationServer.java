import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
        Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24

        AggregationServer.java - a java class that utilises sockets to handle concurrent GET
        and PUT requests.

        AggregationServer.java maintains connection with ContentServer after PUT requests
        have been made, and deletes data after unique ContentServer has disconnected. If AggregationServer.java
        crashes, it ContentServers will wait until it comes back online and send relevant data again.
 */

public class AggregationServer {
    // Initialise variables
    private static final int port = 4567;
    private static final AtomicInteger lamportClock = new AtomicInteger(0); // Lamport Clock
    // Expiry Time; server waits 30000ms (30s), after disconnection to delete data
    private static final long expiry = 30000;
    // weatherData, hashmap to store json data given unique identifier
    private static final ConcurrentHashMap<String, String> weatherData = new ConcurrentHashMap<>();
    // expiryTasks, hashmap to schedule tasks for data expiration given unique identifier
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> expiryTasks = new ConcurrentHashMap<>();
    // scheduler for handling data expiration within a *SEPERATE* thread
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server is running on port " + port);

            // Concurrently and continuously handle any new clients
            while (true) {
                // Accept all client connections (yeah?)
                Socket clientSocket = serverSocket.accept();
                // In a new thread, handle individual clients
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method handleClient() - handles both types of client requests
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // This part is unnecessary but good for debugging multiple GETClients running from different devices
            String clientIpAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Accepted connection from: " + clientIpAddress);

            // Read the first line of the request...
            String requestLine = in.readLine();

            // If request has something,
            if (requestLine != null) {
                // Split by space and extract method
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];

                if (method.equals("PUT")) {
                    // Handle PUT requests in separate function, return unique ID
                    String id = handlePutRequest(in, out);
                    // Content server has made the put request, so maintain periodic connection...
                    maintainConnection(clientSocket, out, in, id);
                } else if (method.equals("GET")) {
                    // Handle GET requests made by GETClients
                    handleGetRequest(requestParts, in, out);
                } else {
                    // Otherwise throw 400 error
                    out.println("400");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method handlePutRequest() - Handles requests made by content server
    private static String handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
        lamportClock.incrementAndGet();
        String id = null;
        StringBuilder jsonData = new StringBuilder();
        String line;

        // Extract ID and read headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("ID:")) {
                // Could directly read ID json here, instead made it custom for easier processing
                id = line.split(":")[1].trim();
            }
        }

        // No id provided! throw a 400 error
        if (id == null || id.isEmpty()) {
            out.println("400: Missing ID");
            return null;
        }

        // Read the JSON data, line by line, until newline (important)
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // Append json data, line by line
            jsonData.append(line).append("\n");
            //System.out.println("Hit!");
        }

        // Store the data in the hashmap with appropriate data
        weatherData.put(id, jsonData.toString());
        // Print out for debugging
        System.out.println("Received Data with ID: " + id);

        // Print out the data for debugging purposes:
        // System.out.println(weatherData.get(id));

        // Respond to the client
        out.println("200");
        out.println("Lamport-Clock: " + lamportClock.get());
        out.flush();

        return id;
    }

    private static void handleGetRequest(String[] requestParts, BufferedReader in, PrintWriter out) {
        lamportClock.incrementAndGet();
        String id = null;
        String line;

        // Extract ID from GET request headers (custom again)
        while (true) {
            try {
                if (!((line = in.readLine()) != null && !line.isEmpty())) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Check if the line contains the ID header
            if (line.startsWith("ID:")) {
                // Extract the ID from the header (ie. "ID: <id-value>")
                id = line.split(":")[1].trim();
            }
        }

        // Check if fail to read, throw a 400
        if (id == null || id.isEmpty()) {
            out.println("400: Missing ID");
            return;
        }

        // Fetch the corresponding data
        String data = weatherData.get(id);

        // Send back finished headers
        out.println("200");
        out.println("Content-Type: application/json");
        out.println("Lamport-Clock: " + lamportClock.get());
        out.println();

        // Send data proper
        if (data != null) {
            out.println(data);
        } else {
            // if data empty -- or random ID, return empty JSON object
            out.println("{}");
        }

        out.flush();
        System.out.println("Data Sent for ID: " + id);
    }

    // Method maintainConnection() - Responsible for periodically checking socket for ContentServer is still open
    private static void maintainConnection(Socket clientSocket, PrintWriter out, BufferedReader in, String id) {
        try {
            // Checks if data is still being sent between two servers
            while (!clientSocket.isClosed() && in.readLine() != null) {
                System.out.println(id + ": Content Server Connection maintained");
                out.println("Maintaining Connection");

                // If there's an expirty task running - cancel it! connection has been re-established for this ID
                if (expiryTasks.containsKey(id)) {
                    expiryTasks.get(id).cancel(false);
                    expiryTasks.remove(id);
                }
            }

            // Otherwise, connection in some way has failed... start timer to deletion!
            System.out.println(id + ": Content Server Connection lost. Starting expiry timer!\n");
            scheduleExpiry(id);

        } catch (SocketException e) {
            // Or if socket has encountered an error, do the same
            System.out.println(id + "Content Server Connection closed, starting expiry timer!\n");
            scheduleExpiry(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method scheduleExpiry - Simply a way to schedule deletion after some delay
    private static void scheduleExpiry(String id) {
        // Schedule a task, expiryTask, to remove data after given time frame (30 seconds)
        ScheduledFuture<?> expiryTask = scheduler.schedule(() -> {
            // Remove data from hashmap
            weatherData.remove(id);
            System.out.println("Data, ID: " + id + " has expired.");
        }, expiry, TimeUnit.MILLISECONDS);
        // Store the scheduled task in expiryTasks
        expiryTasks.put(id, expiryTask);
    }
}
