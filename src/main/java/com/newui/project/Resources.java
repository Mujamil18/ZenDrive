package com.newui.project;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Resources {

    static final String API_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=true&wait=false&fields=*";

    static final String API_KEY = "c48d64b204mshf2811b5da80b022p1f61d8jsn1731302c5576";  // Replace with your API key

    public static String downloadApiUrl = "https://download.zoho.com/v1/workdrive/download/";

    public static String token = "1000.53ce7fe795b2bf246d559c43770ba7b3.27296e73594ec2058e731f5156af7b12";


    private Resources() {

        String tokken = null;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("token_config.json")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                JSONObject json = new JSONObject(jsonBuilder.toString());
                tokken = json.optString("access_token");
                System.out.println("OAuth Token: " + tokken);
            } else {
                System.err.println("Error: token_config.json not found in the classpath.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(tokken);
        token = tokken;
    }

}
