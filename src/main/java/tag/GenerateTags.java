package tag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateTags {

    // Method to connect to the database and get content from a specific table
    public static List<String> getContentFromDatabase() throws SQLException {
        List<String> contentList = new ArrayList<>();
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Database connection (adjust with your actual database details)
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/project", "mujamil", "muji@123");
            stmt = connection.createStatement();
            String sql = "SELECT resource_id, content FROM transcriptText"; // Adjust the table and column names
            rs = stmt.executeQuery(sql);

            // Collecting the content from the DB
            while (rs.next()) {
                contentList.add(rs.getString("content"));
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }

        return contentList;
    }

    // Method to check if a string is valid JSON
    public static boolean isValidJson(String content) {
        try {
            new JSONObject(content); // Try parsing as JSON
            return true;
        } catch (Exception e) {
            return false; // If parsing fails, it's not valid JSON
        }
    }

    // Method to interact with Gemini API and get generated tags
    public static Map<String, String> generateTagsFromGemini(List<String> contentList) throws Exception {
        Map<String, String> resourceTags = new HashMap<>();
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDTzKbSZWAJfuBuoLwTb4uKPMZ6CsCl2_o";
        HttpClient client = HttpClient.newHttpClient();

        // Construct JSON payload with content from the database
        StringBuilder contentBuilder = new StringBuilder();
        for (String content : contentList) {
            if (isValidJson(content)) {
                // Handle JSON content
                JSONObject jsonContent = new JSONObject(content);
                // Extract necessary fields from JSON (assuming a field "text" exists)
                if (jsonContent.has("text")) {
                    contentBuilder.append(jsonContent.getString("text")).append(" ");
                } else {
                    // Handle JSON content without a "text" field if needed
                    contentBuilder.append(content).append(" ");
                }
            } else {
                // Handle plain text content
                contentBuilder.append(content).append(" ");
            }
        }

        // Construct the JSON payload with the content
        String jsonPayload = "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"parts\": [\n" +
                "        {\n" +
                "          \"text\": \"" + contentBuilder.toString().trim() + "\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"text\": \"Generate tags based on the content and give the output as resource_id:tags key-value pair.\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Print the payload for debugging purposes
        System.out.println("JSON Payload: " + jsonPayload);

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        // Send the request and get the response
        HttpResponse<String> apiResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = apiResponse.body();

        // Debugging: Print the entire response to see its structure
        System.out.println("API Response: " + responseBody);

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(responseBody);

        // Debugging: Check if "candidates" exists in the response
        if (jsonResponse.has("candidates")) {
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");

                for (int i = 0; i < parts.length(); i++) {
                    String partText = parts.getJSONObject(i).getString("text");
                    // For simplicity, let's assume each part is a tag with an associated resource_id
                    String resourceId = "resource" + (i + 1); // Mock the resource ID
                    resourceTags.put(resourceId, partText); // Use content as a "tag"
                }
            }
        } else {
            // If "candidates" doesn't exist, print the response for debugging
            System.out.println("No 'candidates' field found in the response.");
        }

        return resourceTags;
    }

    // Method to integrate the entire process: connect to DB, generate tags from Gemini
    public static void main(String[] args) {
        try {
            // Step 1: Get content from the database
            List<String> contentList = getContentFromDatabase();

            // Step 2: Generate tags using Gemini API
            Map<String, String> tags = generateTagsFromGemini(contentList);

            // Step 3: Output the result (key-value pairs for resource_id and tag_name)
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                System.out.println("Resource ID: " + entry.getKey() + ", Tag Name: " + entry.getValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
