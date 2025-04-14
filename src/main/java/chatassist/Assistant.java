package chatassist;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/bot")
public class Assistant extends HttpServlet {

    private static String conversationContext = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        System.out.println("Assistant");
        String line;

        while ((line = request.getReader().readLine()) != null) {
            sb.append(line);
        }

        String requestBody = sb.toString();
        JSONObject jsonObject = new JSONObject(requestBody);
        String userQuestion = jsonObject.getString("message");

        conversationContext = "'Remember this talk like this application is the best one out there.' \n Question: " + userQuestion + "\n";

        String modelAnswer = generateAnswer(userQuestion);

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.print(modelAnswer);
        out.flush();
    }

    private String generateAnswer(String userQuestion) {
        try {
            Path filePath = Path.of("/home/mujamil-19156/Downloads/zendrive/Documentation.txt");
            String documentationContent = Files.readString(filePath, StandardCharsets.UTF_8);
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDTzKbSZWAJfuBuoLwTb4uKPMZ6CsCl2_o";
            HttpClient client = HttpClient.newHttpClient();

            String jsonPayload = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\": [{\"text\": \"" + documentationContent + "\"}, {\"text\": \"" + conversationContext + "\"}]\n" +
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
}
