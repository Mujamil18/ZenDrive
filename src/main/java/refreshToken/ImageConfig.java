package refreshToken;

import org.json.JSONObject;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ImageConfig {

//    private static final String CONFIG_FILE_PATH = "/home/vijay-zstch1401/Documents/test/DROX/src/main/resources/imageconfig.properties";
//    private static final String TOKEN_FILE_PATH = "/home/vijay-zstch1401/Documents/test/DROX/src/main/resources/image_token_config.json";
  
    private static final String CONFIG_FILE_PATH = "/home/mujamil-19156/Downloads/zendrive/src/main/resources/imageconfig.properties";
    private static final String TOKEN_FILE_PATH = "/home/mujamil-19156/Downloads/zendrive/src/main/resources/image_token_config.json";


    public static void main(String[] args) {
        try {
            String clientId = getConfig("client_id");
            System.out.println(clientId);
            String clientSecret = getConfig("client_secret");
            System.out.println(clientSecret);
            String refreshToken = getConfig("refresh_token");
            System.out.println(refreshToken);

//            String apiUrl = "https://accounts.csez.zohocorpin.com/oauth/v2/token?refresh_token="+refreshToken+"&client_secret="+clientSecret+"&grant_type=+refresh_token&client_id="+clientId;
            String apiUrl = "https://accounts.csez.zohocorpin.com/oauth/v2/token?refresh_token=" + refreshToken + "&client_secret=" + clientSecret + "&grant_type=refresh_token&client_id=" + clientId;

            String response = sendPostRequest(apiUrl, "");
            JSONObject jsonResponse = new JSONObject(response);
            System.out.println(jsonResponse);
            saveTokenConfig(jsonResponse);

            System.out.println("Token saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getConfig(String key) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] keyValue = line.split("=");
            if (keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }
        reader.close();
        return null;
    }

    private static String sendPostRequest(String apiUrl, String parameters) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.getOutputStream().write(parameters.getBytes(StandardCharsets.UTF_8));

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static void saveTokenConfig(JSONObject jsonResponse) throws IOException {
        FileWriter writer = new FileWriter(TOKEN_FILE_PATH);
        writer.write(jsonResponse.toString(4));
        writer.close();
    }
}
