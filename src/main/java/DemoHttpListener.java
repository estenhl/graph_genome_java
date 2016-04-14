/**
 * Created by estenhl on 4/12/16.
 */
import java.io.*;
import java.net.InetSocketAddress;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;
        import com.sun.net.httpserver.HttpHandler;
        import com.sun.net.httpserver.HttpServer;

public class DemoHttpListener {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getRawQuery();
            System.out.println("Got request with query " + query);
            String sequences = null;
            String em = null;
            for (String param: query.split("&")) {
                if (param.startsWith("sequences")) {
                    sequences = param.split("=")[1];
                } else if (param.startsWith("error-margin")) {
                    em = param.split("=")[1];
                }
            }

            String response = null;
            if (sequences == null && em == null) {
                response = "Requires a set of sequences and an error-margin";
                System.out.println("response: " + response);
                t.sendResponseHeaders(500, response.length());
            } else if (sequences == null) {
                response = "Requires a set of sequences";
                System.out.println("response: " + response);
                t.sendResponseHeaders(500, response.length());
            } else if (em == null) {
                response = "Requires an error-margin";
                t.sendResponseHeaders(500, response.length());
            } else {
                String pngFile = Long.toString(System.currentTimeMillis());
                String[] c = { "bash", "../build_index.sh", "-is=" + sequences, "-em=" + em, "--png=" + pngFile };
                Process p = Runtime.getRuntime().exec(c);
                System.out.println("p.exitValue(): " + p.exitValue());
                if (p.exitValue() != 0) {
                    Scanner reader = new Scanner(p.getErrorStream());
                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pngFile + ".log")));
                    while (reader.hasNextLine()) {
                        String line = reader.nextLine();
                        writer.write(line + "\n");
                        System.out.println("ERROR: " + line);
                    }
                    reader.close();
                    writer.close();
                }
                System.out.println("Png: " + pngFile + ".png");
                t.sendResponseHeaders(200, response.length());
            }
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
