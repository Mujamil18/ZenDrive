package com.newui.project;

import java.io.BufferedReader;


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.newui.project.Resources;


@WebServlet("/GetSourceCode")
public class GetSourceCode extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("in source");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String tokenn =Resources.token;
        JSONObject jsonResponse = new JSONObject();

        BufferedReader reader = request.getReader();
        StringBuilder codeJsonBuffer = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            codeJsonBuffer.append(line);
        }

        String jsonData = codeJsonBuffer.toString();

        try {

            JSONObject requestBody  = new JSONObject(jsonData);

            String resourceId = requestBody.getString("resourceId");

            String sourceCode = makeApiRequestWithToken(tokenn, resourceId);

            String fileType=requestBody.getString("fileType");

            storeSourceCode(resourceId,sourceCode);

            jsonResponse.put("sourceCode",sourceCode );
            System.out.println(sourceCode);

            response.getWriter().write(sourceCode);

        }catch(Exception e){

            e.printStackTrace();
        }

    }

    /*store to the db*/

    private void storeSourceCode(String resourceId, String sourceCode) {

        Connection con=null;
        ResultSet rs=null;
        java.sql.Statement stmt=null;
        String query;
        PreparedStatement pstmt = null;

        boolean exists = false;

        try {
            con=getDatabaseConnection();

            stmt =con.createStatement();

            query="SELECT * FROM sourceCodes WHERE resource_id = ? LIMIT 1";

            pstmt = con.prepareStatement(query);
            pstmt.setString(1, resourceId);
            rs = pstmt.executeQuery();
            exists=rs.next();

            if(!exists) {

                query="INSERT INTO sourceCodes(resource_id,code) VALUES (?,?);";
                pstmt =con.prepareStatement(query);

                pstmt.setString(1, resourceId);
                pstmt.setString(2, sourceCode);

                int rowsInserted = pstmt.executeUpdate();

            }else {
                //System.out.println("else");
                int id=rs.getInt("id");
                String code=rs.getString("code");

                if(code.equals(sourceCode)) {

                }else {

                    query="UPDATE sourceCodes SET code = ? WHERE id =?;";

                    pstmt.setString(1,sourceCode );
                    pstmt.setInt(2, id);
                    int rowsUpdated = pstmt.executeUpdate();
                }
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
    }



    /*get source code form the zoho work drive via resource id*/

    private String makeApiRequestWithToken(String token, String resourceId) throws IOException {
        String apiUrl = Resources.downloadApiUrl;
        apiUrl = apiUrl + resourceId;
        System.out.println(apiUrl);

        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set the method to GET
        connection.setRequestMethod("GET");

        // Set the Authorization header with the Bearer token
        connection.setRequestProperty("Authorization", "Bearer " + token);

        // Read the API response (assuming it's a text file)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;

            // Read each line from the response and append it to the response StringBuilder
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n"); // Add newline to preserve formatting
            }

            // Return the full content as a single string
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;  // Rethrow the exception so the caller can handle it
        }
    }



    private Connection getDatabaseConnection() throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");
        String jdbcUrl = "jdbc:mysql://localhost:3306/project";
        String username = "mujamil";
        String password = "muji@123";
//        String jdbcUrl = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB"; // Your MySQL database URL ---- WorkDriveDB
//
//	    String username = "WorkDriveProject"; // MySQL username
//	    String password = "workdrive++"; // MySQL password


//        String username = "bharathi_raja"; // MySQL username
//        String password = "raja@123"; // MySQL password

        return DriverManager.getConnection(jdbcUrl, username, password);
    }




}
