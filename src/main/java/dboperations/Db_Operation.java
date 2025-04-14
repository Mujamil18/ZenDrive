package dboperations;

import java.sql.*;


public class Db_Operation {
    String url = "jdbc:mysql://localhost:3306/project";
    String UserName = "mujamil";
    String password = "muji@123";
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;

    public Db_Operation() {
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            conn = DriverManager.getConnection(url, UserName, password);
            this.stmt = conn.createStatement();
            System.out.println("Connection created");
        } catch (SQLException e) {
            System.out.println("+--------------------------------------------+");
            System.out.println("\u001B[32m" + e.getMessage());
            System.out.println("\u001B[0m" + "+--------------------------------------------+");
        }

    }


    public static void main(String[] args) {

    }

    public boolean exeQuery(String query) {


        boolean flag = true;
        try {
            flag = stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public ResultSet excuteQuery(String query) {
        try {
            rs = stmt.executeQuery(query);
        } catch (SQLException e) {
            System.out.println("\033[31m" + e.getMessage());
            System.out.println("\033[0m");
        }
        return rs;
    }


}
