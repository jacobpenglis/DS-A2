
/*
    Assignment 2: Distributed Systems - Jacob Penglis (a1850723) - 20.09.24
    (*Bonus Marks Attempt*)

    JsonSerialiser.java - java class to manually parse specific data from and to Json.

*/

public class JsonSerialiser {

    // method serialisetoJson() - Converts raw txt data to json in specific format (used in ContentServer)
    public static String serialiseToJson(String data) {
        StringBuilder json = new StringBuilder();       // Construct json Object
        json.append("{\n");                             // Start with '{' followed by newline

        // Split raw input data by newlines to process line by line
        String[] lines = data.split("\n");
        // For each line of raw data
        for (String line : lines) {
            // Only consider lines containing a colon ("key:value")
            if (line.contains(":")) {
                // Split into key value pairs
                String[] parts = line.split(":", 2); // <key, val>
                // Extract specific parts
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Append key first, enclosed in quotes
                json.append("    \"").append(key).append("\": ");
                // Check if the line is one of numeric
                if (isNumeric(value)) {
                    // Numbers have no quotations
                    json.append(value);
                } else {
                    // Otherwise, append quotations
                    json.append("\"").append(value).append("\""); // String
                }
                // Always append a comma and newline at end of each line
                json.append(",\n");
            }
        }
        // Go back and remove trailing comma and newline character
        if (json.length() > 1){
            json.setLength(json.length()-2);
        }
        // End json object with newline, "}"
        json.append("\n}");
        return json.toString();
    }

    // Method deserialiseFromJson() - Converts Json to Raw txt (used in GETClient)
    public static String deserialiseFromJson(String jsonData) {
        StringBuilder data = new StringBuilder();             // Text object (to be built within)
        String[] lines = jsonData.split("\n");          // Split data by newline again

        // For each line in json data
        for (String line : lines) {
            // Ignore curly braces!! (start + end)
            if (line.trim().equals("{") || line.trim().equals("}")) {
                continue;
            }

            // Split the JSON into key-value pairs again (KEY:VALUE)
            String[] parts = line.trim().split(":", 2);
            if (parts.length == 2) {
                // Remove unnecessary characters, quotations and commas
                String key = parts[0].trim().replace("\"", ""); // Remove quotes from key
                String value = parts[1].trim().replace(",", "").replace("\"", ""); // Remove quotes from value and trailing commas

                // Reconstruct original TEXT format (KEY:VALUE) without additional spaces
                data.append(key).append(":").append(value).append("\n");
            }
        }

        return data.toString().trim(); // Remove final newline
    }

    // Checks if parsed value is a number (Definitely better way to do this)
    private static boolean isNumeric(String value) {
        // For each character in parsed value
        for (char c : value.toCharArray()) {
            // If character is not a digit and not a decimal point
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        // Otherwise return true!
        return true;
    }
}


