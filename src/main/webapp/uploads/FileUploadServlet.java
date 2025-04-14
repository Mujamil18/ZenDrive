package captureURL;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileUploadServlet extends HttpServlet {
    String resourceId = null;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("UPLOADING");
        String oAuthToken = "Zoho-oauthtoken " + getOAuthToken();
        if (oAuthToken == null) {
            response.getWriter().println("Error: OAuth token not found in token_config.json.");
            return;
        }

        String parentId = getParentID(); // Parent folder ID

        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                FileItem fileItem = upload.parseRequest(request).get(0);

                InputStream fileInputStream = fileItem.getInputStream();
                String filename = fileItem.getName();

                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpPost uploadFile = new HttpPost("https://www.zohoapis.com/workdrive/api/v1/upload?filename=" + filename + "&override-name-exist=true&parent_id=" + parentId);
                uploadFile.addHeader("Authorization", oAuthToken);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addBinaryBody("content", fileInputStream, ContentType.APPLICATION_OCTET_STREAM, filename);

                HttpEntity multipart = builder.build();
                uploadFile.setEntity(multipart);

                HttpResponse responseFromZoho = httpClient.execute(uploadFile);

                if (responseFromZoho.getStatusLine().getStatusCode() == 200) {
                    String responseBody = new String(responseFromZoho.getEntity().getContent().readAllBytes());
                    // Parse the response to get the resource_id
                    JSONObject jsonObject = new JSONObject(responseBody);

                    JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
                    resourceId = data.getJSONObject("attributes").getString("resource_id");
                    System.out.println(data.getJSONObject("attributes"));
                    String fileType = filename.substring(filename.lastIndexOf(".") + 1);
                    System.out.println(fileType);
                    storeVideoMetadata(filename, resourceId, fileType);
                    setPermissionsForFile(resourceId);


                    // Process the file only if it is video/audio
                    if (isAudioFile(fileType) || isVideoFile(fileType)) {
                        System.out.println("File is an audio or video. Sending to transcription API.");
                        int i = 0;
                        while (i < 100000) {
                            i++;
                        }

                        // Download the file and send it to the transcription API
                        String mediaUrl = " https://download.zoho.com/v1/workdrive/download/" + resourceId;
                        String transcription = sendFileToTranscriptionApi(mediaUrl, filename);

                        // Save transcription data to DB
                        storeTranscriptionInDatabase(resourceId, filename, transcription);
                    } else if (isTextOrCodeFile(fileType)) {
                        System.out.println("TEXT/CODE FILE");
                        int i = 0;
                        while (i < 100000) {
                            i++;
                        }
                            // Construct the media URL
                            String mediaUrl = "https://download.zoho.com/v1/workdrive/download/" + resourceId;

                            // Define a local file path to store the downloaded file
                            String filePath = "/home/vijay-zstch1401/IdeaProjects/DROX/DOWNLOADED_FILES/" + resourceId + ".txt"; // Adjust file path and extension as needed

                            // Step 1: Download the file
                            downloadFileFromZoho(mediaUrl, filePath);

                            // Step 2: Read the file and store transcription in DB
                            readFileAndStoreInDatabase(filePath, resourceId, filename);


                    } else if (isImageFile(fileType)) {
                        System.out.println("IMAGE FILE");
                        String mediaUrl = "https://download.zoho.com/v1/workdrive/download/" + resourceId;
                        String filePath = "/home/vijay-zstch1401/IdeaProjects/DROX/DOWNLOADED_FILES/" + resourceId + ".jpg";
                        sendImageToApi(filename);

                    }

                    // Send back resource_id to the frontend
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("resource_id", resourceId);
                    jsonResponse.put("filename", filename);

                    response.setContentType("application/json");
                    response.getWriter().write(jsonResponse.toString());
                }
                else {
                    response.getWriter().println("Error uploading file. Status: " + responseFromZoho.getStatusLine());
                }

                httpClient.close();

            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().println("Error: " + e.getMessage());
            }
        } else {
            response.getWriter().println("Form must have enctype=multipart/form-data.");
        }
    }

    private void sendImageToApi(String fileName) {
        System.out.println("Sending image");

        try {
            // Construct the URL for the API endpoint
            String apiUrl = "https://platformai.csez.zohocorpin.com/internalapi/v2/ai/vision"; // Replace with the actual API URL
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            conn.setRequestMethod("POST");

            // Add headers
            conn.setRequestProperty("Authorization", "Zoho-oauthtoken " + getImageOAuthToken());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("portal_id", "ZS");

            // Enable input/output streams for the request
            conn.setDoOutput(true);

            // Create JSON body with the image URL and model
            String jsonBody = "{\n" +
                    "    \"content\": \"Analyze the image and describe its contents in detail. Does the image include any text? If so, extract and list the text. Identify key objects, scenes, or elements present in the image for better search relevance.\",\n" +
                    "    \"images\": [\n" +
                    "        {\n" +
                    "            \"type\": \"url\",\n" +
                    "            \"url\": \"" + "https://files-accl.zohoexternal.com/public/workdrive-external/download/" + resourceId + "\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"model\": \"chatgpt-4o-latest\"\n" +
                    "}";

            // Write JSON body to the connection output stream
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response from the API
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    // Parse the response JSON
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String transcription = jsonResponse.getJSONObject("data")
                            .getJSONArray("results")
                            .getString(0);  // Extract the first result from the array

                    // Now store the transcription in the database
                    storeTranscriptionInDatabase(resourceId, fileName, transcription);

                }
            } else {
                System.out.println("API request failed with response code: " + responseCode);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getImageOAuthToken() {
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

    public void downloadFileFromZoho(String mediaUrl, String filePath) throws IOException {
        // Create URL object for the media URL
        URL url = new URL(mediaUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set the request method to GET
        connection.setRequestMethod("GET");

        // Set Authorization header with Bearer token
        connection.setRequestProperty("Authorization", "Bearer " + getOAuthToken());

        // Check if the request is successful (HTTP Status Code 200)
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            // Create input stream to read the file
            InputStream inputStream = connection.getInputStream();

            // Create an output stream to write the file to a local path
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File downloaded successfully: " + filePath);
        } else {
            System.out.println("Failed to download file. Response Code: " + connection.getResponseCode());
        }
    }

    public void readFileAndStoreInDatabase(String filePath, String fileId, String fileName) throws IOException, SQLException, ClassNotFoundException {
        // Read the file contents
        StringBuilder fileContents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContents.append(line).append("\n");
            }
        }

        // Store the transcription in the database
        storeTranscriptionInDatabase(fileId, fileName, fileContents.toString());
    }

    private String sendFileToTranscriptionApi(String mediaUrl, String fileName) {
        String transcription = "";
        try {
            // The URL of the transcription API
            URL transcribeUrl = new URL("http://172.21.12.5:5003/transcribe"); // Replace with your actual transcription API URL
            HttpURLConnection conn = (HttpURLConnection) transcribeUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Set the boundary for multipart/form-data
            String boundary = UUID.randomUUID().toString();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                // Write the first part for upload_choice (this is a text parameter)
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"upload_choice\"" + lineEnd).getBytes());
                os.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes());
                os.write(lineEnd.getBytes());
                os.write("upload".getBytes()); // The value for upload_choice
                os.write(lineEnd.getBytes());

                // Write the second part for upload_data (this is the file field)
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"upload_data\"; filename=\"" + fileName + "\"" + lineEnd).getBytes());
                os.write(("Content-Type: application/octet-stream" + lineEnd).getBytes());
                os.write(lineEnd.getBytes());

                // Open the file URL stream directly with Bearer token for authentication
                try {
                    URL fileUrl = new URL(mediaUrl); // mediaUrl is the URL of the file on Zoho WorkDrive
                    HttpURLConnection fileConn = (HttpURLConnection) fileUrl.openConnection();

                    // Add Authorization header with Bearer token
                    fileConn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());
                    fileConn.setRequestMethod("GET");

                    // Open the input stream to the file URL
                    try (InputStream fileStream = fileConn.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fileStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);  // Write the file content directly to the output stream
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error while downloading file from URL: " + mediaUrl);
                }

                // End of the form data
                os.write(lineEnd.getBytes());
                os.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
            }

            // Get response from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            // Parse the transcription response (assuming JSON response)
            JSONObject transcriptionResponse = new JSONObject(responseBuilder.toString());
            transcription = transcriptionResponse.optString("api_output"); // Assuming 'api_output' is the field for transcription
            System.out.println(transcription);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return transcription;
    }

    private void storeVideoMetadata(String fileName, String fileId, String fileType) {
        String jdbcURL = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB";
        String dbUser = "WorkDriveProject";
        String dbPassword = "workdrive++";

        String sql = "INSERT INTO fileInfo(resource_id, Filename, fileType,userName,password,email) VALUES (?, ?, ?, ?, ?, ?)";


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try (Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);
            stmt.setString(2, fileName);
            stmt.setString(3, fileType);
            stmt.setString(4, "LALALALA");
            stmt.setString(5, "LELELELE");
            stmt.setString(6, "someone@mail.com");


            stmt.executeUpdate();
            System.out.println("Video metadata stored successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void storeTranscriptionInDatabase(String fileId, String fileName, String transcription) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String jdbcURL = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB";
        String dbUser = "WorkDriveProject";
        String dbPassword = "workdrive++";

        String sql = "INSERT INTO transcriptText(resource_id, file_name, content,userName) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);
            stmt.setString(2, fileName);
            stmt.setString(3, transcription);
            stmt.setString(4, "I AM VIJAY");

            stmt.executeUpdate();
            System.out.println("Transcription saved to the database.");
        } catch (SQLException e) {
            e.printStackTrace();
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
            } else {
                System.err.println("Error: token_config.json not found in the classpath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
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




    // Helper methods to check audio/video file types
    private boolean isAudioFile(String fileType) {
        return "mp3".equalsIgnoreCase(fileType) ||
                "wav".equalsIgnoreCase(fileType) ||
                "flac".equalsIgnoreCase(fileType) ||
                "aac".equalsIgnoreCase(fileType) ||
                "ogg".equalsIgnoreCase(fileType) ||
                "m4a".equalsIgnoreCase(fileType) ||
                "wma".equalsIgnoreCase(fileType);
    }

    private boolean isVideoFile(String fileType) {
        return "mp4".equalsIgnoreCase(fileType) ||
                "avi".equalsIgnoreCase(fileType) ||
                "mov".equalsIgnoreCase(fileType) ||
                "mkv".equalsIgnoreCase(fileType) ||
                "webm".equalsIgnoreCase(fileType) ||
                "flv".equalsIgnoreCase(fileType) ||
                "wmv".equalsIgnoreCase(fileType);
    }

    private boolean isTextOrCodeFile(String fileType) {
        return "txt".equalsIgnoreCase(fileType) ||
                "java".equalsIgnoreCase(fileType) ||
                "py".equalsIgnoreCase(fileType) ||
                "html".equalsIgnoreCase(fileType) ||
                "css".equalsIgnoreCase(fileType) ||
                "js".equalsIgnoreCase(fileType) ||
                "json".equalsIgnoreCase(fileType) ||
                "xml".equalsIgnoreCase(fileType) ||
                "csv".equalsIgnoreCase(fileType) ||
                "md".equalsIgnoreCase(fileType) ||  // markdown
                "log".equalsIgnoreCase(fileType);   // log files
    }

    private boolean isImageFile(String fileType) {
        return "jpg".equalsIgnoreCase(fileType) ||
                "jpeg".equalsIgnoreCase(fileType) ||
                "png".equalsIgnoreCase(fileType) ||
                "gif".equalsIgnoreCase(fileType) ||
                "bmp".equalsIgnoreCase(fileType) ||
                "tiff".equalsIgnoreCase(fileType) ||
                "webp".equalsIgnoreCase(fileType) ||
                "heif".equalsIgnoreCase(fileType);
    }


    private void setPermissionsForFile(String fileID) {
        try {
            // URL for permissions API to update file permissions
            String permissionsApiUrl = "https://www.zohoapis.com/workdrive/api/v1/permissions";

            // Set up the HTTP connection
            URL url = new URL(permissionsApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // Use POST method to set permissions
            conn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());
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

    private JSONArray fetchFilesFromWorkDrive() {
        JSONArray filesArray = new JSONArray();
        try {
            String apiUrl = "https://www.zohoapis.com/workdrive/api/v1/files/"+getParentID()+"/files?page%5Blimit%5D=50&page%5Boffset%5D=0";  // Use the actual parent ID
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray data = jsonResponse.getJSONArray("data");

            for (int i = 0; i < data.length(); i++) {
                JSONObject file = data.getJSONObject(i);
                JSONObject fileData = file.getJSONObject("attributes");
                System.out.println(fileData);
                String fileName = fileData.getString("name");
                String fileType = fileData.getString("display_html_name");
                int fileSize = fileData.getJSONObject("storage_info").getInt("size_in_bytes");

                JSONObject fileObject = new JSONObject();
                fileObject.put("name", fileName);
                fileObject.put("type", fileType);
                fileObject.put("size", fileSize);
                filesArray.put(fileObject);
                System.out.println(filesArray);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filesArray;
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("in upload get");
        JSONArray files = fetchFilesFromWorkDrive();
        response.setContentType("application/json");
        response.getWriter().write(files.toString());
    }

}
