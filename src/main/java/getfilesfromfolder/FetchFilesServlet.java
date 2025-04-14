package getfilesfromfolder;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FetchFilesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final String apiUrlWithFolderId = "https://www.zohoapis.com/workdrive/api/v1/files/" + getParentID() + "/files?page%5Blimit%5D=50&page%5Boffset%5D=0"; // Replace with your actual API URL
    private ExecutorService executorService;


    @Override
    public void init() throws ServletException {
        super.init();
        executorService = Executors.newFixedThreadPool(10); // Use a fixed thread pool to limit the number of concurrent threads
    }

    @Override
    public void destroy() {
        super.destroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            // Fetch data from the API
            URL url = new URL(apiUrlWithFolderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());

            // Check if the connection was successful (HTTP 200 OK)
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch data: " + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            // Parse the JSON response
            JSONObject responseObject = new JSONObject(responseBuilder.toString());
            JSONArray filesData = responseObject.getJSONArray("data");
            JSONObject firstObject = filesData.getJSONObject(0); // Get first object inside "data"
            JSONObject attributes = firstObject.getJSONObject("attributes");
            String resourceId = attributes.getString("name"); // Extract "resource_id"
            String fileType = attributes.getString("type");

            System.out.println(resourceId);

            List<Future<Void>> futures = new ArrayList<>();

            // Loop through the files in the response
            for (int i = 0; i < filesData.length(); i++) {
                JSONObject file = filesData.getJSONObject(i);
                String fileId = file.getString("id");
                    String mediaUrl = "https://download.zoho.com/v1/workdrive/download/" + fileId;

                // Handle audio/video files
                if (fileType.equals("audio") || fileType.equals("video")) {
                    System.out.println("VIDEO/AUDIO FOUND");
                    String fileName = resourceId;
                    // Submit the transcription task to the executor service
                    futures.add(executorService.submit(new FileTranscribeTask(i + 1, fileId, fileName, mediaUrl)));
                    System.out.println("Task " + i + " submitted");
                }
                // Handle text or stmth file types
                else if (fileType.equals("docs")) {
                    System.out.println("TEXT/STMH FILE FOUND");
                    // Read file content
                    String fileContent = readFileFromUrl(mediaUrl);
                    if (fileContent != null) {
                        // Save content to database
                        saveToDatabase(resourceId, fileContent, fileId);
                    }
                } else {
                    System.out.println("File is not audio, video, text, or stmth");
                }
            }

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();  // Blocking call to ensure all tasks complete
            }

            // Output the success response
            JSONObject successResponse = new JSONObject();
            successResponse.put("message", "All tasks completed successfully.");
            out.println(successResponse);

        } catch (Exception e) {
            e.printStackTrace();

            // Output the error response
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "Failed to fetch files or send transcription request: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(errorResponse);
        } finally {
            out.close();
        }
    }

    private String readFileFromUrl(String fileUrl) {
        StringBuilder fileContent = new StringBuilder();

        try {
            // Open connection to the file URL
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + getOAuthToken());

            // Read the content from the file URL
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return fileContent.toString();
    }

    private void saveToDatabase(String resourceId, String content, String fileName) {
        try (Connection conn = getDatabaseConnection()) {
            // Insert content into the database

            String query = "INSERT INTO transcriptText (resource_id,content,file_name) VALUES (?, ?, ?)"; // Assuming your DB has a table 'textFiles' with a 'content' column
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, resourceId);
                stmt.setString(2, content);
                stmt.setString(3, fileName);
                stmt.executeUpdate();
                System.out.println("Content saved to database.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Error saving content to database: " + e.getMessage());
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

    private Connection getDatabaseConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
//        String jdbcUrl = "jdbc:mysql://10.52.0.201:3306/WorkDriveDB"; // Your MySQL database URL
//        String username = "WorkDriveProject"; // MySQL username
//        String password = "workdrive++"; // MySQL password
        
        String jdbcUrl = "jdbc:mysql://localhost:3306/project";
        String username = "mujamil";
        String password = "muji@123";
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
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

    // Helper class for transcription tasks (optional)
    private class FileTranscribeTask implements Callable<Void> {
        private final int fileIndex;
        private final String fileId;
        private final String fileName;
        private final String mediaUrl;

        public FileTranscribeTask(int fileIndex, String fileId, String fileName, String mediaUrl) {
            this.fileIndex = fileIndex;
            this.fileId = fileId;
            this.fileName = fileName;
            this.mediaUrl = mediaUrl;
        }

        @Override
        public Void call() throws Exception {
            try {
                // Prepare URL and connection for file upload
                URL transcribeUrl = new URL("http://172.21.12.5:5003/transcribe");
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
                String transcription = transcriptionResponse.optString("api_output"); // Assuming 'subtitles' is the field to save

                // Insert the file data and transcription into the database
                saveTranscriptionToDatabase(fileId, fileName, transcription);

                System.out.println("File " + fileIndex + " processed and saved: " + transcription);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("File " + fileIndex + ": Error - " + e.getMessage());
            }
            return null;
        }

        private void saveTranscriptionToDatabase(String fileId, String fileName, String transcription) {
            try (Connection conn = getDatabaseConnection()) {
                Statement st = conn.createStatement();
                st.execute("Delete from transcriptText;");
                String query = "INSERT INTO transcriptText (resource_id, file_name, content) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, fileId);
                    stmt.setString(2, fileName);
                    stmt.setString(3, transcription);
                    stmt.executeUpdate();
                    System.out.println("Transcription for file " + fileId + " saved to the database.");
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Error saving transcription to database: " + e.getMessage());
            }
        }
    }
}
