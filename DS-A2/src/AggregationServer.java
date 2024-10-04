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
    private static final int port = 4567;
    private static final AtomicInteger lamportClock = new AtomicInteger(0); // Lamport Clock
    private static final long expiry = 30000;
    private static final ConcurrentHashMap<String, WeatherData> weatherData = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> expiryTasks = new ConcurrentHashMap<>(); // Track scheduled tasks
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String DATA_START = "(JSON Start)";
    private static final String DATA_END = "(JSON End)";

    public static void main(String[] args) {
        try {
            // Load data from backup.txt on startup
            loadBackupData();  

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Aggregation Server is running on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handleClient() - Checks the sent request and calls the appropriate methods
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String clientIpAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Accepted connection from: " + clientIpAddress);

            String requestLine = in.readLine();

            if (requestLine != null) {
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];

                if (method.equals("PUT")) {
                    String id = handlePutRequest(in, out);
                    if (id != null) {
                        // Schedule data expiration
                        scheduleExpiry(id, expiry);  
                    }
                } else if (method.equals("GET")) {
                    handleGetRequest(requestParts, in, out);
                } else {
                    out.println("400");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handlePutRequest() - Appropriately stores sent data, and updates backup.txt
    private static String handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
        lamportClock.incrementAndGet();
        String id = null;
        StringBuilder jsonData = new StringBuilder();
        String line;
    
        // Extract ID and read headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("ID:")) {
                id = line.split(":")[1].trim();
            }
        }
    
        // No ID provided
        if (id == null || id.isEmpty()) {
            out.println("400");
            return null;
        }
    
        // Read JSON data, preserving the format
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            jsonData.append(line).append("\n");
        }
    
        // Store the data in the map
        WeatherData data = new WeatherData(jsonData.toString(), lamportClock.get());
    
        if (data.jsonData == null || data.jsonData.isEmpty()) {
            out.println("204");
            return null;
        }
    
        if (!(data.jsonData.startsWith("{") || data.jsonData.endsWith("}"))) {
            out.println("500"); // internal server error
            return null;
        }
    
        // Check if the weatherData map has reached its limit of 20 entries
        if (weatherData.size() >= 20) {
            // Find the entry with the lowest lamport clock value (oldest)
            String oldestId = null;
            int lowestLamport = Integer.MAX_VALUE;
    
            for (ConcurrentHashMap.Entry<String, WeatherData> entry : weatherData.entrySet()) {
                if (entry.getValue().lamportClock < lowestLamport) {
                    lowestLamport = entry.getValue().lamportClock;
                    oldestId = entry.getKey();
                }
            }
    
            // Remove the oldest entry from the map
            if (oldestId != null) {
                weatherData.remove(oldestId);
                System.out.println("Removed oldest entry with ID: " + oldestId);
    
                // Cancel the expiration task for the oldest entry
                ScheduledFuture<?> expiredTask = expiryTasks.remove(oldestId);
                if (expiredTask != null) {
                    expiredTask.cancel(false);
                }
    
                // Remove from backup.txt
                removeFromFile(oldestId);
            }
        }
    
        // Add the new data entry to the map
        weatherData.put(id, data);
    
        // Calculate expiration timestamp
        long expirationTimestamp = System.currentTimeMillis() + expiry;
    
        // Write to backup.txt
        writeToFile(id, data, expirationTimestamp);
    
        // Schedule expiration task
        scheduleExpiry(id, expiry);
    
        // Respond to the client
        System.out.println("Data Received.");
    
        if (lamportClock.get() == 1) {
            out.println("201");
        } else {
            out.println("200");
        }
    
        out.println("Lamport-Clock: " + lamportClock.get());
        out.flush();
    
        return id;
    }
    
    // handleGetRequest() - Method to send data to the GETClient based on given StationID
    private static void handleGetRequest(String[] requestParts, BufferedReader in, PrintWriter out) {
        lamportClock.incrementAndGet();
        String id = null;
        String line;
    
        while (true) {
            try {
                if (!((line = in.readLine()) != null && !line.isEmpty())) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    
            if (line.startsWith("ID:")) {
                id = line.split(":")[1].trim();
            }
        }
        
    
        // If no ID is provided or ID is "null", return the most recent data
        if (id == null || id.isEmpty() || "null".equals(id)) {
            out.println("200");
            out.println("Content-Type: application/json");
            out.println("Lamport-Clock: " + lamportClock.get());
            out.println();
    
            int highestLamport = -1;
            WeatherData mostRecent = null;
    
            for (WeatherData data : weatherData.values()) {
                if (data.lamportClock > highestLamport) {
                    mostRecent = data;
                    highestLamport = data.lamportClock;
                }
            }
    
            System.out.println("Null ID: Sending Most-Recent Data");
    
            if (mostRecent != null) {
                out.println(mostRecent.jsonData);  // Correctly send the most recent JSON data
            } else {
                out.println("{}");
            }
    
            out.flush();
            return;
        }
    
        // Fetch the corresponding data if ID was provided
        WeatherData dataObj = weatherData.get(id);
    
        out.println("200");
        out.println("Content-Type: application/json");
        out.println("Lamport-Clock: " + lamportClock.get());
        out.println();
    
        if (dataObj != null && dataObj.jsonData != null) {
            out.println(dataObj.jsonData);
        } else {
            out.println("{}");
        }
    
        out.flush();
        System.out.println("Data Sent for ID: " + id);
    }
    
    // Schedule expiration for data, ensuring only one task per ID
    private static void scheduleExpiry(String id, long timeLeft) {
        // Cancel existing task for this ID, if any
        ScheduledFuture<?> existingTask = expiryTasks.get(id);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
        }

        // Schedule a new expiration task
        ScheduledFuture<?> newTask = scheduler.schedule(() -> {
            weatherData.remove(id);
            try {
                removeFromFile(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Data, ID: " + id + " has expired.");
        }, timeLeft, TimeUnit.MILLISECONDS);

        // Track the new task
        expiryTasks.put(id, newTask);
    }

    // writeToFile() - Method to write to backup.txt for server resilience
    private static void writeToFile(String id, WeatherData data, long expirationTimestamp) throws IOException {
        removeFromFile(id);  // Remove the old entry first

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("backup.txt", true))) {
            writer.write(id + "|" + data.lamportClock + "|" + DATA_START + "\n");
            writer.write(data.jsonData);  // Write the JSON as-is
            writer.write(DATA_END + "|" + expirationTimestamp + "\n");
        }
    }

    // removeFromFile() - Method to remove specific entries from backup.txt
    private static void removeFromFile(String id) throws IOException {
        File inputFile = new File("backup.txt");
        File tempFile = new File("temp_backup.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String currentLine;
            boolean skipJson = false;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith(id + "|")) {
                    skipJson = true;  
                    continue;
                }

                if (skipJson && currentLine.startsWith(DATA_END)) {
                    // Stop skipping when we reach the end of the JSON data
                    skipJson = false;  
                    continue;
                }

                if (!skipJson) {
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
        }

        if (!inputFile.delete()) {
            System.out.println("Could not delete original file");
        }
        if (!tempFile.renameTo(inputFile)) {
            System.out.println("Could not rename temp file");
        }
    }

    // loadBackupData() - Method to read from backup.txt and update server hashmap
    private static void loadBackupData() throws IOException {
        File backupFile = new File("backup.txt");

        if (!backupFile.exists()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // skip via custom delimiter '|'
                if (!line.contains("|")) {
                    continue; 
                }

                String[] parts = line.split("\\|", 3);
                String id = parts[0];
                int lamport = Integer.parseInt(parts[1]);

                // Read JSON data
                StringBuilder jsonData = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.equals(DATA_END)) {
                    jsonData.append(line).append("\n");
                }

                // Read the expiration timestamp, to resume timer
                line = reader.readLine();
                if (line == null || !line.contains("|")) {
                    continue; 
                }

                long expirationTimestamp = Long.parseLong(line.split("\\|")[1]);

                WeatherData data = new WeatherData(jsonData.toString(), lamport);
                weatherData.put(id, data);

                long timeLeft = expirationTimestamp - currentTime;

                if (timeLeft > 0) {
                    scheduleExpiry(id, timeLeft);
                } else {
                    weatherData.remove(id);
                    removeFromFile(id);
                }
            }
        }
    }
}

// Custom Class WeatherData - Each 'entry' into the hashmap should contain the corresponding lamport value
class WeatherData {
    String jsonData;
    int lamportClock;

    public WeatherData(String jsonData, int lamportClock) {
        this.jsonData = jsonData;
        this.lamportClock = lamportClock;
    }
}
