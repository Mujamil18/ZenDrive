package fetchingnotesfromdb;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter("/index.html")  // Applies to all pages
public class AuthenticationFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        System.out.println("Authentication filter called...");

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String path = req.getRequestURI();  // Get requested URL

        if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")
                || path.contains("login.html") || path.contains("signup.html") || path.contains("LoginServlet")) {
            chain.doFilter(request, response);
            return;
        }


        // ❌ Block unauthorized access
        if (session == null || session.getAttribute("userName") == null) {
            System.out.println("Unauthorized access! Redirecting to login.");
            res.sendRedirect("auth.html");
        } else {
            System.out.println("User authenticated. Proceeding to page.");
            chain.doFilter(request, response); // Allow access
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {}
    public void destroy() {}
}
