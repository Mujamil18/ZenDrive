package getembed;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ZohoWorkDriveServlet extends HttpServlet {

    // Main method to process files and apply permissions
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // In ZohoWorkDriveServlet
//        String resourceId = request.getParameter("resource_id");

        // Folder API URL to fetch files in the folder
        String apiUrlWithFolderId = "https://www.zohoapis.com/workdrive/api/v1/files/uxzqy4c926549a6d345fabbe6671db401796b/files?page%5Blimit%5D=50&page%5Boffset%5D=0"; // Replace {folderId} with actual folder ID

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            // Get OAuth token
            String oauthToken = getOAuthToken();

            // Fetch the files in the folder
            URL url = new URL(apiUrlWithFolderId); // Replace with the actual folder ID
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + oauthToken);

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            // Parse the response to get the files data
            JSONObject responseObject = new JSONObject(responseBuilder.toString());
            JSONArray filesData = responseObject.getJSONArray("data");

            // Process each file using its fileID
            for (int i = 0; i < filesData.length(); i++) {
                JSONObject file = filesData.getJSONObject(i);
                String fileID = file.getString("id"); // Extract the fileID

                // Call the API to set permissions for the file using its fileID
                setPermissionsForFile(fileID, oauthToken);
            }

            // Respond back with a success message
            out.print("{\"status\":\"success\", \"message\":\"Files processed and permissions set successfully.\"}");

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    // Helper method to fetch OAuth token
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

    // Method to set permissions for a file using its fileID
    private void setPermissionsForFile(String fileID, String oauthToken) {
        try {
            // URL for permissions API to update file permissions
            String permissionsApiUrl = "https://www.zohoapis.com/workdrive/api/v1/permissions";

            // Set up the HTTP connection
            URL url = new URL(permissionsApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // Use POST method to set permissions
            conn.setRequestProperty("Authorization", "Bearer " + oauthToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Prepare the JSON body for permissions
            String jsonInputString = "{"
                    + "\"data\": {"
                    + "  \"attributes\": {"
                    + "    \"resource_id\": \"" + fileID + "\","
                    + "    \"shared_type\": \"publish\","
                    + "    \"role_id\": \"34\""
                    + "  },"
                    + "  \"type\": \"permissions\""
                    + "}"
                    + "}";

            // Send the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            // Process the response (log or handle as needed)
            JSONObject response = new JSONObject(responseBuilder.toString());
            System.out.println("Permissions set for file ID " + fileID + ": " + response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
