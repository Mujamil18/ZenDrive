package generateImage;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet("/generateImage")
public class ImageGeneration extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Add CORS headers to the response
        System.out.println("IN IMAGE");
        response.setHeader("Access-Control-Allow-Origin", "*");  // Allow all origins (for development, can restrict in production)
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");  // Allow POST and OPTIONS methods
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");  // Allow Content-Type header

        // Handle preflight OPTIONS request (common with cross-origin requests)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Parse the JSON content from the incoming request
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }

        JSONObject requestBody = new JSONObject(content.toString());
        String prompt = requestBody.optString("prompt"); // Get the prompt from the request body

        if (prompt == null || prompt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Prompt is missing or empty.");
            return;
        }

        // URL of the API to which we want to send the request
        String apiUrl = "https://platformai.csez.zohocorpin.com/internalapi/v2/ai/image"; // Replace with the actual API URL for image generation

        // Set up the connection
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set the request method to POST
        conn.setRequestMethod("POST");

        // Add headers
        conn.setRequestProperty("Authorization", "Zoho-oauthtoken " + getOAuthToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("portal_id", "ZS"); // Add portal_id header

        // Enable input/output streams for POST request body
        conn.setDoOutput(true);

        // Create the JSON body content for the API request
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("content", prompt);
        jsonBody.put("quality", "hd");
        jsonBody.put("size", "1792x1024");
        jsonBody.put("model", "dall-e-3");

        // Send the JSON body to the API
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Get the response code
        int statusCode = conn.getResponseCode();

        if (statusCode == 200) {
            // Read the response from the image generation API
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseContent = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseContent.append(inputLine);
            }
            in.close();

            // Parse the response JSON (API response)
            JSONObject apiResponse = new JSONObject(responseContent.toString());

            // Extract the image URL from the response (assuming "results" contains the URL)
            JSONObject data = apiResponse.getJSONObject("data");
            String imageUrl = data.getJSONArray("results").getString(0); // Assuming the first result

            // Set the response type to JSON
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();

            // Prepare JSON response with the image URL
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("imageUrl", imageUrl);

            // Send the image URL back to the client
            out.print(jsonResponse.toString());
            out.flush();
        } else {
            // Handle API failure (non-200 response code)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Image generation API request failed with status code: " + statusCode);
        }

        // Close the connection
        conn.disconnect();
    }


    // Method to get OAuth token from a configuration file
    private String getOAuthToken() {
        String token = null;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("image_token_config.json")) {
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
                System.err.println("Error: image_token_config.json not found in the classpath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }
}
