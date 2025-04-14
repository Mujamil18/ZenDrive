package readimage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class ExtractText {

    public static void main(String[] args) {
        // Paths to the images you want to process
        String imagePath1 = "image1.jpg";
        String imagePath2 = "image2.jpg";
        String apiKey = "AIzaSyDTzKbSZWAJfuBuoLwTb4uKPMZ6CsCl2_o";  // Replace with your actual API key

        try {
            // Base64 encode the images
            String base64Image1 = encodeImageToBase64(imagePath1);
            String base64Image2 = encodeImageToBase64(imagePath2);

            // Prepare the JSON request payload
            String jsonPayload = prepareJsonPayload(base64Image1, base64Image2);

            // Send the POST request to Gemini API
            sendPostRequest(jsonPayload, apiKey);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String encodeImageToBase64(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        byte[] fileContent = new byte[(int) imageFile.length()];

        try (FileInputStream fileInputStream = new FileInputStream(imageFile)) {
            fileInputStream.read(fileContent);
        }

        return Base64.getEncoder().encodeToString(fileContent);
    }

    private static String prepareJsonPayload(String base64Image1, String base64Image2) {
        return "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"parts\": [\n" +
                "        {\n" +
                "          \"inline_data\": {\n" +
                "            \"mime_type\": \"image/jpeg\",\n" +
                "            \"data\": \"" + base64Image1 + "\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"inline_data\": {\n" +
                "            \"mime_type\": \"image/png\",\n" +
                "            \"data\": \"" + base64Image2 + "\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"text\": \"Extract text from the images.\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private static void sendPostRequest(String jsonPayload, String apiKey) throws IOException {
        String apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        int retries = 3;
        while (retries > 0) {
            try {
                // Create a URL object
                URL url = new URL(apiEndpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set request method to POST
                connection.setRequestMethod("POST");

                // Set Content-Type and Accept headers
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                // Enable input/output streams
                connection.setDoOutput(true);

                // Send the JSON payload
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Read the response
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.out.println("Response: " + response.toString());
                }
                break; // Exit the loop if the request is successful
            } catch (IOException e) {
                retries--;
                if (retries == 0) {
                    throw e; // If no retries left, throw the exception
                }
                System.out.println("Request failed. Retrying...");
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
