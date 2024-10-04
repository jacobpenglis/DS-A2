Assignment 2: Distributed Systems
Jacob Penglis (a1850723)
20.09.24

How to Run (from parent directory):
1. 'make compile'       - Compiles all files in /bin
2. 'make run_server'    - Runs the Aggregation Server
3. 'make run_content'   - Runs Content Server with Port & Filename (Specified in Makefile)
4. 'make run_get'       - Runs GETClient with Port & StationID (Specified in Makefile)
5. 'make clean'         - Deletes binary files

How to Run Tests
1. make test            - Compiles all test files in /bin
2. make run_tests       - Runs every test specified in /test


**Implementation Details**


*AggregationServer.java:*

The Aggregation Server (AS) opens a socket and continously listens for any request. 

If the request is 'PUT' the AS will store the sent data into a hashmap, and will delete the data after 30 seconds. If the data is 'updated' (by sending with same StationID) then the expiration timer will restart. If the data doesn't make sense, the AS will return a '500' error code, otherwise any other issue will return a '400' error code. If the data is successfully handled, the AS will return a '201' for the first entry, and '200' for each entry afterwards.

If the request is 'GET' the AS will send the stored data that the 'GET' request refers to via Station ID. Notably, if the ID is null/empty, then the AS will send the most-recent entry. Successfully sending a GET request will return a '200' code, if the data is empty the AS will return a '204' code, and if an error occurs the AS will return '400'.

The AS will not store more than 20 entries, and will rewrite the oldest data entry if this number is exceeded. The AS will also write all entries to a data file called 'backup.txt', whereby, in the case of the AS shutting down, it will read the data (and its properties) from the data file. This allows for the AS to be resilient even if it fails and restarts.

The AS maintains a lamport clock - which it stores with each data entry, ensuring that even upon restart the lamport value is not lost.


*ContentServer.java: <http://localhost:4567> file_to_path*

The Content Server (CS) attempts to open a socket (Arg 1) and send a 'PUT' request to store data (Arg 2) specified from the command line.

The CS will read the data from a .txt file, and serialise it to .json using the built serialiser. The CS, will send the serialised json data to the AS via an open socket. The connection closes once a '201' or '200' code is sent back. Otherwise, upon a '400/500' code, the CS will attempt to re-send the data 3 times before closing.

If connection with the AS can't be established, the CS will attempt to reconnect three times before closing.


*GETClient.java: <http://localhost:4567> Station ID*

The GET Client attempts to open a socket (Arg 1) and send a 'GET' request to retrive some data based on its Station ID (Arg 2).

The GET Client, upon a successful response (200), will de-serialise the data from .json using the built json serialiser. It will then print the data to the user. Upon a failure response (400), will attempt to resend the GET request 3 times before closing. 


*JsonSerialiser.java (BONUS MARKS ATTEMPT):*

An in-built serialiser that manually edits, line by line, weather data in the given format, and parses it to JSON. 
