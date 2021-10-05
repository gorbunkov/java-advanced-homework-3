package homework.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MyClient {

    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 9999;

    private static final Logger log = LoggerFactory.getLogger(MyClient.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("Application expects 2 arguments: command and argument");
            System.exit(0);
        }
        String command = args[0];
        String argument = args[1];
        new MyClient().start(command, argument);
    }

    private void start(String command, String argument) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
        ) {
            out.println(createCommandJson(command, argument));
//            out.println(createCommandJson("zip", "/Users/gorbunkov/Work/trainings"));
//            out.println(createCommandJson("download", "https://www.haulmont.com|https://www.haulmont.ru|https://www.google.com"));
//            out.println(createCommandJson("sum", "2000"));
            String response = in.readLine();
            log.info("Server response: {}", response);
        } catch (IOException e) {
            log.error("Error on opening the socket connection", e);
        }
    }

    private String createCommandJson(String command, String argument) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("command", command);
        rootNode.put("argument", argument);
        return rootNode.toString();
    }

}
