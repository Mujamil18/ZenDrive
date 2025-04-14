package thumbnail;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet("/ThumbnailServlet")
public class ThumbnailServlet extends HttpServlet {

    // Define your Bearer Token here in Java
    private final String BEARER_TOKEN = getOAuthToken(); // Replace with your actual token

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Log the incoming request data
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }

        System.out.println("Request Body: " + requestBody);

        JSONObject requestData = new JSONObject(requestBody.toString());
        String fileID = requestData.getString("fileID");
        System.out.println("File ID: " + fileID);


        // Construct the thumbnail URL for Zoho API
        String thumbnailUrl = "https://previewengine-accl.zoho.com/thumbnail/WD/" + fileID;

        try {
            // Log the thumbnail URL before making the request
            System.out.println("Making request to Zoho API for thumbnail: " + thumbnailUrl);

            URL url = new URL(thumbnailUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + BEARER_TOKEN);

            // Get the response code to check if it's successful
            int responseCode = connection.getResponseCode();

            System.out.println("Zoho API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Set the response content type to the image MIME type (it could be PNG, JPEG, etc.)
                String contentType = connection.getContentType();
                response.setContentType(contentType);

                // Get the image input stream from Zoho API and write it to the response output stream
                try (InputStream inputStream = connection.getInputStream();
                     OutputStream outputStream = response.getOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    System.out.println(inputStream);
                }

            } else {
                // Log error and return the error message
                System.err.println("Error: Unauthorized or bad response from Zoho API. Response code: " + responseCode);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"Forbidden: Invalid Token or Access Denied\"}");
            }
        } catch (Exception e) {
            // Handle any exceptions that occur during the HTTP request
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Error fetching thumbnail: " + e.getMessage() + "\"}");
        }
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
