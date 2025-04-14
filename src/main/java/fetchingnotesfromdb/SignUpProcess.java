package fetchingnotesfromdb;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.authorization.SQLConnection;

import dboperations.Db_Operation;
@WebServlet("/signUp")
public class SignUpProcess extends HttpServlet {
	SQLConnection  sq= new SQLConnection();
	
	public void doPost(HttpServletRequest req,HttpServletResponse res) {
		
		System.out.println("signup servlet called");
		System.out.println("signup servlet called........");
		String name=req.getParameter("Name");
		String email=req.getParameter("email");
		String password=req.getParameter("password");
		HttpSession session=req.getSession();
		 if(!sq.signUp(name, password, email)) {
			 System.out.println("inserted successfully");  
			 session.setAttribute("userName", name);
			 session.setAttribute("password", password);
		 }
        
         try {
			res.sendRedirect("index.html");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}

