package com.authorization;
import java.sql.Statement;
import java.io.IOException;
//import java.io.PrintWriter;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.ResultSetMetaData;
//import java.sql.SQLException;
//import java.io.BufferedReader;
//import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

@WebServlet("/loginWorkdrive")
public class LoginProcess extends HttpServlet {
    SQLConnection connect=new SQLConnection();


    public void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException  {

//	    res.setContentType("application/json");
        System.err.println("loginProcess called.....");
        String userName=req.getParameter("username");
        String password=req.getParameter("password");


        if(connect.isValid(userName,password)) {
            HttpSession session=req.getSession();
            session.setAttribute("userName",userName);
            session.setAttribute("password", password);
            session.setMaxInactiveInterval(600);
            res.sendRedirect("index.html");
        }
        else {
            res.sendRedirect("auth.html");
        }


    }


}

