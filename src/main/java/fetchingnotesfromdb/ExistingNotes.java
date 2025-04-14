package fetchingnotesfromdb;

import dboperations.Db_Operation;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/existNotes")
public class ExistingNotes extends HttpServlet {
    Db_Operation db = new Db_Operation();
    ResultSet rs = null;
    int getuserId = 0;

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        System.out.println("existingNotes called...");
        HttpSession session = req.getSession(false);
        StringBuilder sb = new StringBuilder();

        if (session == null || session.getAttribute("userName") == null) {
            res.sendRedirect("loginPage.html"); // Redirect if session expired
            System.out.println("no session maintain");
            return;
        }


        BufferedReader reader = req.getReader();

        PrintWriter out = res.getWriter();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        JSONObject json = new JSONObject(sb.toString());
        String resourceId = json.getString("id");

        System.out.println("resourceId... from db onclick.." + resourceId);
        String userName = (String) session.getAttribute("userName");
        System.out.println("login User......" + userName);
        String getUserIdQuery = "SELECT user_id FROM workdriveUsers WHERE userName ='" + userName + "';";
        rs = db.excuteQuery(getUserIdQuery);
        String excnote = "";
        try {
            while (rs.next()) {
                getuserId = rs.getInt("user_id");

            }
            ResultSet rs1 = existingNotes(getuserId, resourceId);

            while (rs1.next()) {
//	    		System.out.println("existing notes......"+rs1.getString("notes"));
                System.out.println("existing notes from database......." + excnote);

                excnote = rs1.getString("notes");

            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        json.put("existNotes", excnote);
        System.out.println(json.get("existNotes"));
        res.setContentType("application/json");
        System.out.println(json.getString("existNotes"));

        res.getWriter().write(json.toString());

    }

    public ResultSet existingNotes(int user_id, String resourceId) {
        String existNotes = "select notes from userNotes where user_id=" + user_id + " and  resource_id='" + resourceId + "';";
        System.out.println(existNotes);
        ResultSet rs = db.excuteQuery(existNotes);
        return rs;
    }

}