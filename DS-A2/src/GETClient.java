import java.io.*;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.Socket;

/*
        Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24

        GETClient.java - a java class that utilises sockets to receive data; after sending a 'GET' request
        to the Aggregation Server. GETClient will also deserialize the data received using in build serialiser
        and displays it to the user.

        Upon failing to connect, GETClient will attempt to reconnect three times before stopping.
 */

public class GETClient {
    // Initialise variables
    private static String server;
    private static int port;                   
    private static String id;           
    private static final long delay = 3000;                 
    private static final int noRetries = 3;    
    private static final AtomicInteger lamportClock = new AtomicInteger(0);       

    public static void main(String[] args){
        int retries = 0;          
        boolean success = false;   

        if (args.length == 2){
            GETClient.id = args[1];  
        } else {
            GETClient.id = null;
        }
        
        try {
            String url = args[0];

            if (!url.startsWith("http://")) {
                throw new IllegalArgumentException("Usage: java GETClient <http://server:port> <stationID>");
            }

            url = url.substring(7);

            String[] parts = url.split(":");

            GETClient.server = parts[0];
            GETClient.port = Integer.parseInt(parts[1]);

        } catch (Exception e) {
            System.out.println("Bad URL");
            e.printStackTrace();
            return;
        }

        // Retry loop (similar to contentServer)
        while (retries < noRetries && !success) {
            // Send GET request and get success feedback
            success = sendRequest();
            // Upon failure...
            if (!success) {
                // Increment
                retries++;
                System.out.println("Retrying... Attempt No: " + retries);
                try{
                    // Delay before retrying
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Method sendRequest() - sends a GET request to AggregationServer
    private static boolean sendRequest(){
            // Try to create a new socket connection to the Aggregation Server
            try (Socket socket = new Socket(server, port);
                 // Output stream
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 // Input stream
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // GET Headers (Specified by assignment)
                out.println("GET /weather.json HTTP/1.1");
                if (id != null) {
                    out.println("ID: " + id); // Only send the ID header if it's not null
                }
                out.println("User-Agent: GETClient/1.0");
                out.println("");
                out.flush();

                String response;
                boolean failedRequest = false;

                // Iterate through response
                while ((response = in.readLine()) != null) {
                    if (response.startsWith("Lamport-Clock: ")){
                        int newLamport = Integer.parseInt(response.split(": ")[1]);
                        lamportClock.set(Math.max(lamportClock.get(),newLamport));
                        lamportClock.set(lamportClock.get() + 1);
                    }
                    if (response.startsWith("{}")){
                        System.out.println(response);
                    } else {
                        System.out.println(JsonSerialiser.deserialiseFromJson(response));
                    }
                    if (response.contains("400")) { // can do more here w/ other errors...
                        failedRequest = true;
                    }
                }

                // Handle failed sockets
                if (failedRequest){
                    System.out.println("Request failed: Trying Again!");
                    return false;
                }
                return true;
            }
            catch (ConnectException a){
                // Connection failed
                System.out.println("400");
                return false;
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }

    }
}
