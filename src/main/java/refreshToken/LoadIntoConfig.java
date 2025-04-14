package refreshToken;

import org.json.JSONObject;



import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoadIntoConfig {

//    private static final String CONFIG_FILE_PATH = "/home/vijay-zstch1401/Documents/test/DROX/src/main/resources/config.properties";
//    private static final String TOKEN_FILE_PATH = "/home/vijay-zstch1401/Documents/test/DROX/src/main/resources/token_config.json";
    
    private static final String CONFIG_FILE_PATH = "/home/mujamil-19156/Zendrive/src/main/resources/config.properties";
    private static final String TOKEN_FILE_PATH = "/home/mujamil-19156/Zendrive/src/main/resources/token_config.json";
//    private static final long EXPIRATION_TIME = 45 * 60 * 1000; // 45 minutes in milliseconds
    
    public LoadIntoConfig() {
    	try {
            String clientId = getConfig("client_id");
            String clientSecret = getConfig("client_secret");
            String refreshToken = getConfig("refresh_token");
            String apiUrl = "https://accounts.zoho.com/oauth/v2/token";
            String parameters = "refresh_token=" + refreshToken + "&client_secret=" + clientSecret + "&grant_type=refresh_token&client_id=" + clientId;

            String response = sendPostRequest(apiUrl, parameters);
            JSONObject jsonResponse = new JSONObject(response);
            System.out.println(response);
            
            addTimestamp(jsonResponse);
            saveTokenConfig(jsonResponse);

            System.out.println("Token saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public static void main(String[] args) {
//        try {
//            String clientId = getConfig("client_id");
//            String clientSecret = getConfig("client_secret");
//            String refreshToken = getConfig("refresh_token");
//            String apiUrl = "https://accounts.zoho.com/oauth/v2/token";
//            String parameters = "refresh_token=" + refreshToken + "&client_secret=" + clientSecret + "&grant_type=refresh_token&client_id=" + clientId;
//
//            String response = sendPostRequest(apiUrl, parameters);
//            JSONObject jsonResponse = new JSONObject(response);
//            System.out.println(response);
//            
//            addTimestamp(jsonResponse);
//            saveTokenConfig(jsonResponse);
//
//            System.out.println("Token saved successfully");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

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
    
    private static void addTimestamp(JSONObject jsonResponse) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
        jsonResponse.put("timestamp", timestamp);
    }
//    public String getOAuthToken() {
//        String token = null;
//        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TOKEN_FILE_PATH)) {
//            if (inputStream != null) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                StringBuilder jsonBuilder = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    jsonBuilder.append(line);
//                }
//
//                JSONObject json = new JSONObject(jsonBuilder.toString());
//                token = json.optString("access_token");
//                String timestamp = json.optString("timestamp");
//
//                if (timestamp != null) {
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//                    Date tokenDate = sdf.parse(timestamp);
//                    long currentTime = System.currentTimeMillis();
//                    long tokenTime = tokenDate.getTime();
//
//                    // Check if the token is expired or will expire soon
//                    if ((currentTime - tokenTime) > EXPIRATION_TIME) {
//                        System.out.println("Token expired or will expire soon. Refreshing...");
//                        refreshToken(); // Directly refresh the token by invoking the method
//                    } else {
//                        System.out.println("good to go");
//                    }
//                }
//            } else {
//                System.err.println("Error: token_config.json not found in the classpath.");
//            }
//        }
//        catch(Exception e) {
//        	e.printStackTrace();
//        }
//        return token;
//    }
//    private void refreshToken() {
//        try {
//            // This will trigger the logic inside LoadIntoConfig when the class is instantiated
//            new LoadIntoConfig(); // Calling the constructor directly, which will execute the refresh logic
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
