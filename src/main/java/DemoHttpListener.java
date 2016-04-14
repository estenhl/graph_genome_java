/**
 * Created by estenhl on 4/12/16.
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Base64;

import javax.imageio.ImageIO;

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
      String alignment = null;
      for (String param : query.split("&")) {
        if (param.startsWith("sequences")) {
          sequences = param.split("=")[1];
        } else if (param.startsWith("error-margin")) {
          em = param.split("=")[1];
        } else if (param.startsWith("alignment")) {
          alignment = param.split("=")[1];
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
        System.out.println(
            "Starting process with sequences " + sequences + ", em: " + em + " and align: "
                + alignment);
        String[] build = { "bash", "build_index.sh", "--index=" + pngFile + ".index",
            "-is=" + sequences, "-em=" + em, "--png=" + pngFile };
        Process p = Runtime.getRuntime().exec(build);
        int err = 0;
        try {
          err = p.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        if (alignment != null) {
          System.out.println("Doing alignment!");
          String[] align = { "bash", "align_sequence.sh", "--index=" + pngFile + ".index",
              "-as=" + alignment, "-em=" + em, "--png=" + pngFile };
          p = Runtime.getRuntime().exec(align);
          try {
            err = p.waitFor();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        BufferedImage image = null;
        File file = new File(pngFile + ".png");
        image = ImageIO.read(file);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        System.out.println("Image: ");
        ImageIO.write(image, "png", byteArray);
        byte[] byteImage = byteArray.toByteArray();
        String dataImage = Base64.getEncoder().encodeToString(byteImage);

        response = "mycallback({png: \"" + URLEncoder.encode(dataImage, "UTF-8") + "\"});";
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Content-Type", "application/json; charset=utf8");
        t.sendResponseHeaders(200, response.length());
      }
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }

  }
}
