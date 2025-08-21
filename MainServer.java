package shafi;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MainServer {

    private static final String URL = "jdbc:mysql://localhost:3306/students";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        // Load MySQL Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve HTML pages
        server.createContext("/admin", new HtmlHandler(System.getProperty("user.dir") + "/resources/admin.html"));
        server.createContext("/student", new HtmlHandler(System.getProperty("user.dir") + "/resources/student.html"));

        // Handle form submissions
        server.createContext("/set", new SetHandler());
        server.createContext("/get", new GetHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started at http://localhost:8080");
    }

    // Handler to serve HTML files
    static class HtmlHandler implements HttpHandler {
        private final String path;

        public HtmlHandler(String path) {
            this.path = path;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // Handler to insert student data
    static class SetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);

            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                String sql = "INSERT INTO studentsdetails(name, rollno, dataofbirth, firstlanguage, thirdlanguage, mathematics, science, secondlanguage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, params.get("name"));
                ps.setString(2, params.get("rollno"));
                ps.setString(3, params.get("dataofbirth"));
                ps.setInt(4, Integer.parseInt(params.get("firstlanguage")));
                ps.setInt(5, Integer.parseInt(params.get("thirdlanguage")));
                ps.setInt(6, Integer.parseInt(params.get("mathematics")));
                ps.setInt(7, Integer.parseInt(params.get("science")));
                ps.setInt(8, Integer.parseInt(params.get("secondlanguage")));

                int rows = ps.executeUpdate();
                String response = rows > 0 ? "<h1>Submitted successfully</h1>" : "<h1>Submission failed</h1>";

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (SQLException e) {
                String response = "<h1>Error: " + e.getMessage() + "</h1>";
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Handler to fetch student data
    static class GetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);

            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                String sql = "SELECT * FROM studentsdetails WHERE name=? AND rollno=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, params.get("name"));
                ps.setString(2, params.get("rollno"));
                ResultSet rs = ps.executeQuery();

                String response;
                if (rs.next()) {
                    response = "<h2>Name: " + rs.getString("name") + "</h2>"
                            + "<h2>Roll No: " + rs.getString("rollno") + "</h2>"
                            + "<h2>Date of Birth: " + rs.getString("dataofbirth") + "</h2>"
                            + "<h2>First Language: " + rs.getInt("firstlanguage") + "</h2>"
                            + "<h2>Third Language: " + rs.getInt("thirdlanguage") + "</h2>"
                            + "<h2>Mathematics: " + rs.getInt("mathematics") + "</h2>"
                            + "<h2>Science: " + rs.getInt("science") + "</h2>"
                            + "<h2>Second Language: " + rs.getInt("secondlanguage") + "</h2>";
                } else {
                    response = "<h2>No record found</h2>";
                }

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (SQLException e) {
                String response = "<h1>Error: " + e.getMessage() + "</h1>";
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Helper to parse query string
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> map = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    map.put(pair[0], pair[1]);
                }
            }
        }
        return map;
    }
}
