package transcript;

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

@WebServlet("/transcribe")  // The URL pattern for the servlet
public class TranscriptionServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the content type of the response
        response.setContentType("application/json");

        // Get the media URL from the incoming request (for example from a form field)
        String mediaUrl = request.getParameter("media_url");
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = response.getWriter()) {
                out.println("{\"error\": \"media_url parameter is missing\"}");
            }
            return;
        }

        // Prepare the JSON payload
        String jsonPayload = "{\"media_url\": \"" + mediaUrl + "\"}";

        // Create a HttpClient instance
        HttpClient client = HttpClient.newHttpClient();

        // Create an HTTP request to the API endpoint
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://10.51.25.57:9867/transcribe"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            // Send the request and get the response
            HttpResponse<String> apiResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // If the response code is 200 (OK), parse and return the API response
            if (apiResponse.statusCode() == 200) {
                String apiResponseBody = apiResponse.body();
                JSONObject jsonResponse = new JSONObject(apiResponseBody);

                // Write the API response to the servlet's response
                try (PrintWriter out = response.getWriter()) {
                    out.println(jsonResponse.toString(4));  // Pretty print with indent
                }

            } else {
                // Handle non-200 response
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = response.getWriter()) {
                    out.println("{\"error\": \"API request failed with status " + apiResponse.statusCode() + "\"}");
                }
            }
        } catch (Exception e) {
            // Handle exceptions and return a failure message
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.println("{\"error\": \"An error occurred: " + e.getMessage() + "\"}");
            }
            e.printStackTrace();
        }
    }
}
