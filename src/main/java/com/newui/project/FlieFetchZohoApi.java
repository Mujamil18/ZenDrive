package com.newui.project;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.newui.project.Resources;

@WebServlet("/getCodeFromApi")
public class FlieFetchZohoApi extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");


        HashMap<String, String> extn_map = new HashMap<String, String>();

        extn_map.put("java", "java");
        extn_map.put("py", "Python");
        extn_map.put("cpp", "C++");
        extn_map.put("c", "C");
        extn_map.put("js", "JavaScrpit");

        JSONArray allDataArray=new JSONArray();

        JSONArray allCodeArray=new JSONArray();

        JSONObject allCodeObjects=new JSONObject();


        try {

            allDataArray=getDbData();

            for(int i=0;i<allDataArray.length();i++) {

                JSONObject jsonObject = allDataArray.getJSONObject(i);

                String fileType = jsonObject.getString("fileType");


                if(extn_map.containsKey(fileType)) {

                    String resourceId = jsonObject.getString("resource_id");
                    String filename = jsonObject.getString("Filename");
                    String id = jsonObject.getString("id");
                    String uploadTime = jsonObject.getString("uploadTime");

                    fileType = jsonObject.getString("fileType");
                    JSONObject jsonCodeObject = new JSONObject();

                    jsonCodeObject.put("id", id);
                    jsonCodeObject.put("resourceId", resourceId);
                    jsonCodeObject.put("filename", filename);
                    jsonCodeObject.put("fileType", fileType);
                    jsonCodeObject.put("uploadTime", uploadTime);
                    allCodeArray.put(jsonCodeObject);

                }

            }

            allCodeObjects.put("AllCodeFiles",allCodeArray );

            response.getWriter().write(allCodeObjects.toString());

        }catch(Exception e) {

            e.printStackTrace();
        }
    }


    private JSONArray getDbData() {

        Connection con=null;
        ResultSet rs=null;
        java.sql.Statement stmt=null;
        JSONArray jsonArray = new JSONArray();

        try {

            con=getDatabaseConnection();

            stmt =con.createStatement();

            String query="SELECT * FROM fileInfo;";

            rs = stmt.executeQuery(query);

            int columnCount = rs.getMetaData().getColumnCount();

            String colName=null;
            String colValue=null;

            while(rs.next()) {

                JSONObject jsonObject = new JSONObject();

                for(int i=1;i<=columnCount;i++) {

                    String columnName = rs.getMetaData().getColumnName(i);
                    String columnValue = rs.getString(i);
                    jsonObject.put(columnName, columnValue);
                }

                jsonArray.put(jsonObject);
            }

            rs.close();
            stmt.close();
            con.close();

        } catch(Exception e) {
            e.printStackTrace();
        }

        return jsonArray;
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
