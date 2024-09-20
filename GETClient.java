import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

/*
        Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24

        GETClient.java - a java class that utilises sockets to receive data, after sending a 'GET' request
        to the Aggregation Server. GETClient will also deserialize the data received using in build serialiser.

 */

public class GETClient {
    // Initialise variables
    private static final String server = "localhost";       // Server host
    private static final int port = 4567;                   //
    private static final String id = "IDS60901";            // Static ID unique to each client (i think)
    private static final long delay = 3000;                 // delay before another attempt
    private static final int noRetries = 3;                 // No of reconnects

    public static void main(String[] args){
        int retries = 0;            // Number of retry attempts
        boolean success = false;    // Success of GET request

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
                out.println("ID: " + id); // Custom header for ease
                //out.println("HOST: " + server + ":" + port);
                out.println("User-Agent: GETClient/1.0");
                out.println("");
                out.flush();

                String response;
                boolean failedRequest = false;

                // Iterate through response
                while ((response = in.readLine()) != null) {
                    // Using inbuilt parser, deserialise data
                    System.out.println(JsonSerialiser.deserialiseFromJson(response));
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
