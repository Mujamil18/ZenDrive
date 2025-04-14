package com.newui.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.newui.project.Resources;


@WebServlet("/CompileCode")
public class CodeCompileApi extends HttpServlet {
    private static final long serialVersionUID = 1L;


    String API_URL=Resources.API_URL;
    String API_KEY=Resources.API_KEY;

    Connection con=null;
    ResultSet rs=null;
    java.sql.Statement stmt=null;
    String query;
    PreparedStatement pstmt = null;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String tokenn=Resources.token;
        JSONObject jsonResponse = new JSONObject();

        BufferedReader reader = request.getReader();
        StringBuilder codeJsonBuffer = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            codeJsonBuffer.append(line);
        }

        String jsonData = codeJsonBuffer.toString();

        try {

            JSONObject requestAll= new JSONObject(jsonData);
            JSONObject requestBody = requestAll.getJSONObject("selectedFile");

            String resourceId = requestBody.getString("resourceId");
            String fileType=requestBody.getString("fileType");
//            String userInput =requestAll.getString("input");
            String name=requestBody.getString("filename");


            con=getDatabaseConnection();

            stmt =con.createStatement();

            query="SELECT * FROM sourceCodes WHERE resource_id = ? ;";

            pstmt = con.prepareStatement(query);

            pstmt.setString(1,resourceId);

            rs = pstmt.executeQuery();

            String souceCode =null;

            if (rs.next()) {

                souceCode = rs.getString("code");

            }

            String correctCodeToCompileApi="";

            // sent  correct code to judge0 api

            String commonClassName="Main";

            StringBuilder results = new StringBuilder();

            if(fileType.equals("java")){

                String className=name.substring(0,name.lastIndexOf("."));
                correctCodeToCompileApi=souceCode.replaceAll("\\b" + className + "\\b", commonClassName);

                correctCodeToCompileApi= correctCodeToCompileApi.replace(" {", "\n    {\n")
                        .replace("} catch", "\n    } catch")
                        .replace("} finally", "\n    } finally")
                        .replaceAll("}", "\n}");


            }else if(fileType.equals("py")){

                System.out.println("sourccceee "+souceCode);
                String[] lines = souceCode.split("\n");

                for (String line1 : lines) {
                    // Skip lines that start with '#' after trimming whitespace
                    if (!line1.trim().startsWith("#")) {
                        results.append(line1).append("\n");
                    }
                }

                correctCodeToCompileApi=results.toString();

            }else if(fileType.equals("c")){

                correctCodeToCompileApi = souceCode.replaceAll("#include <[^>]+>", "$0\n\n");

            }else if(fileType.equals("cpp")){

                correctCodeToCompileApi=souceCode.replaceAll("#include <(.*?)>", "#include <$1>\n");

                // Ensure `using namespace std;` is on a new line
                correctCodeToCompileApi = correctCodeToCompileApi.replace("using namespace std;", "using namespace std;\n");

                // Separate function definitions correctly
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll("(// Function.*?)int", "$1\n\nint"); // Ensure newlines before function

                // Ensure proper indentation within functions
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll("\\{", "{\n    "); // Ensure newlines after `{`
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll(";", ";\n    ");  // Ensure newlines after `;`

                // Ensure variables are declared correctly in main()
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll("int sum = add(num1, num2);", "    int sum = add(num1, num2);");  // Indentation for sum

                // Fix misplaced indentation at the end
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll("\\s+\\}", "\n}");

                // Remove excessive spaces introduced by the formatter
                correctCodeToCompileApi = correctCodeToCompileApi.replaceAll("\\n\\s+\\n", "\n\n");

            }else {

                correctCodeToCompileApi=souceCode;
            }


            String encodedSourceCode = Base64.getEncoder().encodeToString(correctCodeToCompileApi.getBytes());

            String token = submitCode(fileType,encodedSourceCode);

            if (token == null) {

                jsonResponse.put("status", "Error");
                jsonResponse.put("message", "Failed to submit code.");
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            }

            JSONObject executionResult = fetchExecutionResult(token);

            if (executionResult == null) {
                jsonResponse.put("status", "Error");
                jsonResponse.put("message", "Failed to retrieve execution result.");
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            }

            String encodedOutput = executionResult.optString("stdout", null);
            String output = encodedOutput != null ? new String(Base64.getDecoder().decode(encodedOutput)) : "No Output";

            int code=executionResult.getJSONObject("status").getInt("id");

            if(code !=3) {

                jsonResponse.put("status", executionResult.getJSONObject("status").getString("description"));
                jsonResponse.put("result", executionResult.optString("compile_output", "No output available"));
//	            System.out.println(executionResult.getJSONObject("status").getString("description"));

            }else {

                jsonResponse.put("status", executionResult.getJSONObject("status").getString("description"));
                jsonResponse.put("output", output);
                jsonResponse.put("time", executionResult.getString("time"));

            }

            String status=jsonResponse.getString("status");
            String result;

            if(status.equals("Accepted")) {
                result=jsonResponse.getString("output");
                jsonResponse.put("finalOutput",result);

            }else {

                String decodedResult="";
                result=jsonResponse.getString("result");

                if(result.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                    //System.out.println("enter ");
                    decodedResult= new String(Base64.getDecoder().decode(result));

                }else {
                    decodedResult=result;
                }

                jsonResponse.put("finalOutput",decodedResult);
            }


            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*judge api*/

    private String submitCode(String fileType,String encodedCode) throws IOException {

        JSONObject requestBody = new JSONObject();

        HashMap<String,Integer > langCode=new HashMap<>();
        langCode.put("cpp",54);
        langCode.put("py",71);
        langCode.put("js",93);
        langCode.put("java",62);
        langCode.put("c", 50);

        int langId=langCode.get(fileType);

        if(!(fileType.equals("null"))) {

            requestBody.put("language_id", langId);
            requestBody.put("source_code", encodedCode);

        }
//        else {
//
//            requestBody.put("language_id", langId);
//            requestBody.put("source_code", encodedCode);
////            requestBody.put("stdin", userInput);  // <-- Add user input here
//
//        }

        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-rapidapi-key", API_KEY);
        connection.setRequestProperty("x-rapidapi-host", "judge0-ce.p.rapidapi.com");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try  {
            OutputStream os = connection.getOutputStream();
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }catch(Exception e) {
            e.printStackTrace();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }

        br.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.optString("token", null);
    }

    private JSONObject fetchExecutionResult(String token) throws IOException, InterruptedException {

        String fetchUrl = "https://judge0-ce.p.rapidapi.com/submissions/" + token + "?base64_encoded=true&fields=*";
        int statusCode = 1; // Default to "In Queue"
        JSONObject responseJson = null;

        while (statusCode == 1 || statusCode == 2) {
            HttpURLConnection connection = (HttpURLConnection) new URL(fetchUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("x-rapidapi-key", API_KEY);
            connection.setRequestProperty("x-rapidapi-host", "judge0-ce.p.rapidapi.com");

            // Read response
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            br.close();

            // Parse JSON response
            responseJson = new JSONObject(response.toString());
            statusCode = responseJson.getJSONObject("status").getInt("id");

            // Wait before the next request to avoid excessive API calls
            if (statusCode == 1 || statusCode == 2) {
                Thread.sleep(1000); // Wait for 1 second before retrying
            }
        }
        return responseJson;
    }

    private Connection getDatabaseConnection() throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");
        String jdbcUrl = "jdbc:mysql://localhost:3306/project";
        String username = "mujamil";
        String password = "muji@123";
//        String jdbcUrl = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB"; // Your MySQL database URL ---- WorkDriveDB
//        String username = "WorkDriveProject"; // MySQL username
//        String password = "workdrive++"; // MySQL password

//        String username = "bharathi_raja"; // MySQL username
//        String password = "raja@123"; // MySQL password

        return DriverManager.getConnection(jdbcUrl, username, password);
    }



}
