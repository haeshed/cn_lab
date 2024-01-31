import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;

public class ResponseUtil {
    
     // Request line:
    //      GET /index.html HTTP/1.1

     // Headers: 
            // Host: localhost:8080
            // User-Agent: Chrome/90.0.4430.212
            // Content-Type: text/html
            
    // Body: 
    //      <html><body><h1>404 Not Found</h1></body></html>

    public static void sendSuccessResponse(File file, String contentType, OutputStream out) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());

        PrintWriter writer = new PrintWriter(out, true);
        
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length);
        writer.println(); // Blank line between headers and content
        writer.flush();

        out.write(content);
        out.flush();
    }

    // No content sent for HEAD request 
    public static void sendHeadResponse(File file, String contentType, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + file.length());
        writer.println(); // No body sent for HEAD request
        writer.flush();
    }
    
}
