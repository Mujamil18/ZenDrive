package com.authorization;

import java.sql.*;

public class SQLConnection {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    ResultSetMetaData rsmd = null;
    PreparedStatement pstmt = null;
    boolean loginSuccess = false;

    public SQLConnection() {
        System.out.println("inSQL");
//        String url = "jdbc:mysql://10.52.0.66:3306/WorkDriveDB";
        String url = "jdbc:mysql://localhost:3306/project";
        String UserName = "mujamil";
        String password = "muji@123";


        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, UserName, password);
            stmt = conn.createStatement();
            System.out.println("connection created");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("+--------------------------------------------+");
            System.out.println("\u001B[32m" + e.getMessage());
            System.out.println("\u001B[0m" + "+--------------------------------------------+");
        }
    }

    public static void main(String[] args) {


    }

    public boolean isValid(String userName,String password) {
        String query = "SELECT * FROM workdriveUsers WHERE userName = ? AND passWord = ?;";
        try {
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1,userName );
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();



            if (rs.next()) {
                loginSuccess = true;

            } else {
                loginSuccess=false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loginSuccess;
    }

    public boolean signUp(String name,String password,String email) {
        String insertQuery="insert into workdriveUsers (userName,passWord,email) values (?,?,?); ";
        boolean flag=true;

        try {
            pstmt=conn.prepareStatement(insertQuery);
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            flag=pstmt.execute();
            System.out.println("inserted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return flag;

    }

}
