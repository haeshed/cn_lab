import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientHandler implements Runnable {

    
    private final Socket socket;
    private final String rootDirectory;
    private final String defaultPage;

    public ClientHandler(Socket socket, String rootDirectory, String defaultPage) {
        this.socket = socket;
        this.rootDirectory = rootDirectory;
        this.defaultPage = defaultPage;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                Errors.sendErrorResponse(out, 400); // Bad Request
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                Errors.sendErrorResponse(out, 400); // Bad Request
                return;
            }

            String method = requestParts[0];
            String uri = requestParts[1].equals("/") ? defaultPage : requestParts[1];
            System.out.println("Request: " + method + " " + uri	);
            switch (method) {
                case "GET":
                    handleGetRequest(uri, out);
                    break;
                case "HEAD":
                    handleHeadRequest(uri, out);
                    break;
                case "POST":
                    handlePostRequest(uri, in, out); 
                    break;
                case "TRACE":
                    handleTraceRequest(requestLine, in, out); 
                    break;
                default:
                    Errors.sendErrorResponse(out, 501);
        

            } 
        } catch (FileNotFoundException e) {

                try {

                    Errors.sendErrorResponse(socket.getOutputStream(), 404); // Not Found
                } catch (IOException ex) {

                    ex.printStackTrace();
                }
                } catch (IOException e) {

                    e.printStackTrace(); 

                    if (!socket.isClosed()) {
                        try {
                            OutputStream out = socket.getOutputStream();
                            Errors.sendErrorResponse(out, 500); // Send a 500 Internal Server Error response
                        } catch (IOException ex) {
                            ex.printStackTrace(); // Log this exception as well, in case sending the error response fails
                        }
                    }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace(); // Log exception
            }
        }
    }

    private void handleGetRequest(String uri, OutputStream out) throws IOException {
        Path filePath = Paths.get(rootDirectory).resolve(uri.substring(1)).normalize();
        File file = filePath.toFile();

        if (!file.exists()) {
            Errors.sendErrorResponse(out, 404); // Not Found
            return;
        }

        if (!file.getCanonicalPath().startsWith(new File(rootDirectory).getCanonicalPath())) {
            Errors.sendErrorResponse(out, 403); // Forbidden
            return;
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream"; // Default binary content type
        }

        ResponseUtil.sendSuccessResponse(file, contentType, out);
    }

    private void handleHeadRequest(String uri, OutputStream out) throws IOException {
        Path filePath = Paths.get(rootDirectory).resolve(uri.substring(1)).normalize();
        File file = filePath.toFile();

        if (!file.exists() || !file.getCanonicalPath().startsWith(new File(rootDirectory).getCanonicalPath())) {
            Errors.sendErrorResponse(out, 404); // Not Found or Forbidden
            return;
        }

        String contentType = Files.probeContentType(filePath);
        ResponseUtil.sendHeadResponse(file, contentType, out);
    }

    private void handlePostRequest(String uri, BufferedReader in, OutputStream out) throws IOException {
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nPOST request processed.";
        out.write(response.getBytes());
        out.flush();
    }

    private void handleTraceRequest(String requestLine, BufferedReader in, OutputStream out) throws IOException {
        StringBuilder response = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: message/http\r\n\r\n");
        response.append(requestLine).append("\r\n");
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            response.append(headerLine).append("\r\n");
        }
        out.write(response.toString().getBytes());
        out.flush();
    }
}
