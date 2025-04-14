package generatenotes;

import com.google.api.client.json.Json;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/generateNotes")
public class NoteGeneratorServlet extends HttpServlet {

    private static String generateAnswer(String content) {
        try {
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDTzKbSZWAJfuBuoLwTb4uKPMZ6CsCl2_o";
            HttpClient client = HttpClient.newHttpClient();
            System.out.println(content);

            String jsonPayload = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\": [{\"text\": \"" + content + "\"}, {\"text\": \"Generate notes based on the content (notes should be in points and should cover all important things without '*' also make sure to generate 3 questions with answers to check my understanding about the content also make sure you give response like a html page i will directly insert this to my html make sure you include all the tags and styling to make it look beautiful just give me the required divs alone use inline styles also highlight Question and answers text)\"}]\n" +
                    "  }]\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> apiResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = apiResponse.body();

            JSONObject jsonResponse = new JSONObject(responseBody);
            System.out.println(jsonResponse);
            String modelAnswer = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            return modelAnswer;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String extractSummaryFromJson(String content) {
        try {
            JSONObject jsonContent = new JSONObject(new JSONTokener(content));
            if (jsonContent.has("transcript")) {
                System.out.println(content);
                return jsonContent.getString("transcript");
            }
            return content;
        } catch (Exception e) {
            return content;
        }
    }

    private String getContentFromDatabase(String id) {
        String content = null;
//        String dbUrl = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB";
//        String dbUser = "WorkDriveProject";
//        String dbPassword = "workdrive++";
        String dbUrl = "jdbc:mysql://localhost:3306/project";
        String dbUser = "mujamil";
        String dbPassword = "muji@123";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String query = "SELECT content FROM transcriptText WHERE resource_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, id);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    String response = resultSet.getString("content");

                    try {
                        JSONObject jso = new JSONObject(response);
                        content = jso.getString("transcript");
                    } catch (Exception e1) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            content = response; // Assuming it's a valid JSON array.
                        } catch (Exception e2) {
                            content = response; // Assume it's plain text.
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    private String replaceDoubleQuotesWithSingle(String content) {
        return content.replace("\"", "'");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        BufferedReader br = request.getReader();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        JSONObject json = new JSONObject(sb.toString());
        String id = json.getString("content");

        if (id == null || id.isEmpty()) {
            response.getWriter().write("{\"error\": \"ID parameter is required\"}");
            return;
        }

        String content = getContentFromDatabase(id);

        if (content == null || content.isEmpty()) {
            response.getWriter().write("{\"error\": \"Content not found for the given ID\"}");
            return;
        }

        String processedContent = extractSummaryFromJson(content);

        // Replace double quotes with single quotes
        processedContent = replaceDoubleQuotesWithSingle(processedContent);

        String modelAnswer = generateAnswer(processedContent);

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("notes", modelAnswer);

        response.getWriter().write(jsonResponse.toString());
    }
}
