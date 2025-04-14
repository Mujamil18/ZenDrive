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

@WebServlet("/saveNotes")
public class SaveNotesToDb extends HttpServlet {
    Db_Operation db = new Db_Operation();
    ResultSet rs = null;

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        System.out.println("saveNotesSERVLET CALLED");
        int userIdFromDB = 0;
        HttpSession session = req.getSession(false);
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        if (session == null || session.getAttribute("userName") == null) {
            // Redirect if session expired
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
        String resourceId = json.getString("resourceId");
        System.out.println("id saveButton......." + resourceId);
        String note = json.getString("note");

        String userName = (String) session.getAttribute("userName");
        System.out.println("login User......" + userName);
        String getUserIdQuery = "SELECT user_id FROM workdriveUsers WHERE userName ='" + userName + "';";
        rs = db.excuteQuery(getUserIdQuery);
        try {
            while (rs.next()) {
                userIdFromDB = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ResultSet rs1 = existingNotes(userIdFromDB, resourceId);
        try {
            if (rs1.next()) {
                isConcatNotesTodb(userIdFromDB, resourceId, note);
            } else {

                if (!saveNotes(userIdFromDB, resourceId, note)) {
                    System.out.println("notes saved successfully...");
                }

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        res.setContentType("application/json");

        res.getWriter().write(json.toString());
    }


    public ResultSet existingNotes(int user_id, String resourceId) {
        String existNotes = "select notes from userNotes where user_id=" + user_id + " and  resource_id='" + resourceId + "';";
        System.out.println(existNotes);
        System.out.println("existNotes... method called");
        ResultSet rs = db.excuteQuery(existNotes);
        System.out.println(rs);
        return rs;
    }

    public boolean saveNotes(int user_id, String resourceId, String note) {
        String query = "insert into userNotes (user_id,resource_id,notes) values (" + user_id + ",'" + resourceId + "','" + note + "');";
        System.out.println(query);
        System.out.println("saveNOtes method....." + query);
        boolean flag = db.exeQuery(query);
        return flag;
    }

    public boolean isConcatNotesTodb(int user_id, String resourceId, String notes) {
        notes.replace("\n", "</br>");
        String isExistQuery = "UPDATE userNotes SET notes='" + notes + "' WHERE user_id=" + user_id + " AND resource_id='" + resourceId + "';";

        boolean flag = db.exeQuery(isExistQuery);
        System.out.println(flag + "   .......isExist");
        System.out.println("notes......" + notes);
        System.out.println("meta data concat successfully......");
        return flag;
    }
}