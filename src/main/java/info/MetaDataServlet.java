package info;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet("/fetchMetadata")
public class MetaDataServlet extends HttpServlet {


    String fileName;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("INMETADATA CLASS");
        // Set response content type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Read the JSON body from the POST request
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        JSONObject requestObject = new JSONObject(requestBody.toString());
        fileName = requestObject.getString("fileName");

        try {
            JSONObject metadata = getFileMetadata(fileName);
            System.out.println(metadata);
            // Return the metadata as a JSON response
            response.getWriter().write(metadata.toString());
        } catch (IOException e) {
            // In case of failure, return an error response
            System.out.println(e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Failed to fetch metadata\"" + e + "}");
        }
    }

    private JSONObject getFileMetadata(String fileName) throws IOException {
        // The URL you used in Postman

        URL url = new URL("https://www.zohoapis.com/workdrive/api/v1/files/" + getParentID() + "/files");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Adding the correct Authorization header, just like in Postman
        conn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());

        // Log the request for debugging
        System.out.println("Request URL: " + url);
        System.out.println("Authorization: Bearer " + getOAuthToken());

        // Check if the connection was successful (HTTP 200 OK)
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // Log the response code and body for debugging
            System.err.println("Failed to fetch data. HTTP Response Code: " + responseCode);

            System.err.println("Error response: ");
            throw new IOException("Failed to fetch data: " + responseCode);
        }

        // If successful, read the response
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            responseBuilder.append(inputLine);
        }
        in.close();


        // Check if the response content type is JSON, either 'application/json' or 'application/vnd.api+json'
        String contentType = conn.getContentType();
        if (!(contentType.contains("application/json") || contentType.contains("application/vnd.api+json"))) {
            System.err.println("Expected JSON response, but received: " + contentType);
            throw new IOException("Expected JSON response, but got: " + contentType);
        }

        // Parse the response body
        JSONObject responseObject = new JSONObject(responseBuilder.toString());
        System.out.println(responseObject);
        // Extract the "data" array from the response
        JSONArray filesData = responseObject.getJSONArray("data");

        // Iterate over the array to find the file matching the requested name
        for (int i = 0; i < filesData.length(); i++) {
            JSONObject fileObject = filesData.getJSONObject(i);
            System.out.println(fileObject);
            // Get the "attributes" object inside the file object
            JSONObject attributes = fileObject.getJSONObject("attributes");
            System.out.println(attributes);
            // Extract the file name from the "attributes"
            String currentFileName = attributes.getString("name");
            System.out.println(currentFileName);
            // Compare with the requested file name
            if (currentFileName.equals(fileName)) {
                // File found, return its metadata
                return fileObject;  // Return the full metadata for the file
            }
        }

        // If file not found, throw an exception
        throw new IOException("File not found: " + fileName);
    }


    private String getParentID() {
        String id = null;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("folder.json")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                JSONObject json = new JSONObject(jsonBuilder.toString());
                id = json.optJSONObject("data").optString("id");
            } else {
                System.err.println("Error: folder.json not found in the classpath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    private String getOAuthToken() {
        String token = null;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("token_config.json")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                JSONObject json = new JSONObject(jsonBuilder.toString());
                token = json.optString("access_token");
                System.out.println("OAuth Token: " + token);
            } else {
                System.err.println("Error: token_config.json not found in the classpath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }
}