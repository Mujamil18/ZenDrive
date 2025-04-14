package fetchingnotesfromdb;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("logout servlet called...........");
        HttpSession session=request.getSession(false);
        request.removeAttribute("userName");

        JSONObject json = new JSONObject();
        json.put("logout","logouted out....");
        if (session != null) {
            System.out.println("jksjksjkdjs");
            session.removeAttribute("userName"); // Remove specific attribute
            session.invalidate(); // Destroy session

        }
        response.sendRedirect("login.html");

        response.getWriter().write(json.toString());
    }

}