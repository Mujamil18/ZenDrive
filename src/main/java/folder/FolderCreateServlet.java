package folder;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet("/createFolder")
public class FolderCreateServlet extends HttpServlet {
    // Function to save the response to a file (folder.json)
    public static void saveResponseToFile(String filename, String response) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Extract folder name from the request
        String folderName = request.getParameter("folderName");

        if (folderName == null || folderName.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Folder name is required.");
            return;
        }

        // Call method to create the folder and get the response
        String result = createFolder(folderName);

        // Save the response to a file (folder.json)
//        saveResponseToFile("/home/vijay-zstch1401/IdeaProjects/DROX/src/main/resources/folder.json", result);
        
        saveResponseToFile("/home/mujamil-19156/Downloads/zendrive/src/main/resources/folder.json", result);

        // Set the response content type and send the result back to the client
        response.setContentType("application/json");
        response.getWriter().write(result);
    }

    private String createFolder(String folderName) {
        String apiUrl = "https://www.zohoapis.com/workdrive/api/v1/files";
        String jsonPayload = "{\n" +
                "  \"data\": {\n" +
                "    \"attributes\": {\n" +
                "      \"name\": \"" + folderName + "\",\n" +
                "      \"parent_id\": \"8ff1c7d11721b3ff8471b80ff3eb4cdbe3176\"\n" +
                "    },\n" +
                "    \"type\": \"files\"\n" +
                "  }\n" +
                "}";

        try {
            HttpURLConnection connection = makePostRequest(apiUrl, jsonPayload);
            int responseCode = connection.getResponseCode();

            // If successful response (201 Created)
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                // Check if input stream is not null
                InputStream inputStream = connection.getInputStream();
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    // Return response as a string
                    return response.toString();
                } else {
                    return "No response body received from the server.";
                }
            } else {
                // Handle error response from the server
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    return "Error Response: " + errorResponse.toString();
                } else {
                    return "No error stream received from the server.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
    }

    // Function to make a POST request to the API
    public HttpURLConnection makePostRequest(String apiUrl, String jsonPayload) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + getOAuthToken());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send the request payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        return connection;
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
