package search;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/search") // This annotation maps the servlet to /search URL
public class SearchServlet extends HttpServlet {

    // Database connection details
//    private static final String JDBC_URL = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB";
//    private static final String JDBC_USER = "WorkDriveProject";
//    private static final String JDBC_PASSWORD = "workdrive++";
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/project";
	private static final String JDBC_USER = "mujamil";
	private static final String JDBC_PASSWORD = "muji@123";

    // Do POST request to handle search functionality
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Search Servlet reached");  // Debugging line

        // Get the search query and filter type from request parameters
        String searchQuery = request.getParameter("searchQuery");
        String filter = request.getParameter("filter");  // New filter parameter (all, filename, content)
        System.out.println(searchQuery);
        System.out.println(filter);

        // Check if search query is null or empty, return error if so
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sendErrorResponse(response, "Search query is empty.");
            return;
        }

        // Convert search query to lowercase for case-insensitive search
        searchQuery = searchQuery.toLowerCase();

        // If filter is null or not recognized, default to "all"
        if (filter == null || (!filter.equals("all") && !filter.equals("filename") && !filter.equals("content"))) {
            filter = "all"; // Default filter
        }

        // Set response headers for CORS
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonResponse = new JSONObject();
        JSONArray resultsArray = new JSONArray();

        try {
            // Load JDBC driver and connect to the database
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {

                // Prepare the SQL query based on selected filter
                String sql;
                if (filter.equals("filename")) {
                    sql = "SELECT * FROM transcriptText WHERE file_name LIKE ?";
                } else if (filter.equals("content")) {
                    sql = "SELECT * FROM transcriptText WHERE content LIKE ?";
                } else {
                    sql = "SELECT * FROM transcriptText WHERE content LIKE ? OR file_name LIKE ?";
                }

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%" + searchQuery + "%");
                    if (filter.equals("all")) {
                        ps.setString(2, "%" + searchQuery + "%");
                    }

                    // Execute the query
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String content = rs.getString("content");
                            String videoName = rs.getString("file_name");
                            String videoID = rs.getString("resource_id");

                            // Try to parse content as JSON, if not valid handle as non-JSON
                            JSONObject contentObj = new JSONObject();
                            try {
                                contentObj = new JSONObject(content); // Attempt to parse as JSON
                            } catch (JSONException e) {
                                System.out.println("Content is not a valid JSON. Handling as plain text.");
                                contentObj.put("content", content); // Store plain text in JSON format under "content"
                            }

                            // Process the content if it's JSON or text
                            if (contentObj.has("phrases")) {
                                JSONArray phrases = contentObj.getJSONArray("phrases");
                                for (int i = 0; i < phrases.length(); i++) {
                                    JSONObject segment = phrases.getJSONObject(i);
                                    double segmentStartTime = segment.getDouble("start_time");
                                    double segmentEndTime = segment.getDouble("end_time");
                                    String segmentTranscript = segment.getString("segment_transcript").toLowerCase();

                                    // Check if the search query matches the segment transcript
                                    if (segmentTranscript.contains(searchQuery)) {
                                        // Prepare the result JSON object
                                        JSONObject resultObj = new JSONObject();
                                        resultObj.put("start_time", segmentStartTime);
                                        resultObj.put("end_time", segmentEndTime);
                                        resultObj.put("video_name", videoName);
                                        resultObj.put("video_link", "https://workdrive.zohoexternal.com/embed/" + videoID + "?start=" + segmentStartTime + "&hideui=true");

                                        // Add the result to the response array
                                        resultsArray.put(resultObj);
                                    }
                                }
                            } else {
                                // If no 'phrases' found (non-JSON or other format), match in content
                                String plainContent = contentObj.getString("content");
                                if (plainContent.toLowerCase().contains(searchQuery)) {
                                    JSONObject resultObj = new JSONObject();
                                    resultObj.put("video_name", videoName);
                                    resultObj.put("video_link", "https://workdrive.zohoexternal.com/embed/" + videoID);
                                    resultsArray.put(resultObj);
                                }
                            }
                        }

                        // Prepare the final JSON response with results
                        if (resultsArray.length() > 0) {
                            jsonResponse.put("results", resultsArray);
                        } else {
                            jsonResponse.put("results", new JSONArray());  // Empty array if no results found
                        }

                        // Send the response
                        response.getWriter().write(jsonResponse.toString());
                    }
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                sendErrorResponse(response, "Database error: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found: " + e.getMessage());
            sendErrorResponse(response, "JDBC Driver not found.");
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            sendErrorResponse(response, "An unexpected error occurred.");
        }
    }

    // Helper method to send error response with appropriate error message
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", errorMessage);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(errorResponse.toString());
    }

}
