package homework.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import homework.server.command.DownloadCommand;
import homework.server.command.SumCommand;
import homework.server.command.ZipCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyServer {

    public static final int SERVER_PORT = 9999;

    private final static int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    //Executor that dispatches incoming requests
    //max threads = 100
    private ExecutorService dispatcherExecutor = new ThreadPoolExecutor(10, 100, 60,
            TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    //Executor for CPU consuming tasks
    //max threads = N of processors
    private ExecutorService cpuExecutor = new ThreadPoolExecutor(2, AVAILABLE_PROCESSORS, 60,
            TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    //Executor for IO intensive tasks
    //max threads = 100
    private ExecutorService ioExecutor = new ThreadPoolExecutor(10, 100, 60,
            TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    private static final Logger log = LoggerFactory.getLogger(MyServer.class);

    public static void main(String[] args) {
        new MyServer().start();
    }

    private void start() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                dispatcherExecutor.execute(() -> handleRequest(socket));
            }
        } catch (IOException e) {
            log.error("Error on socket creation", e);
        }
    }

    private void handleRequest(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {
            String requestBody = in.readLine();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(requestBody);
            JsonNode commandNode = rootNode.get("command");
            if (commandNode == null) {
                log.error("Command is not specified in the request");
                return;
            }
            JsonNode argumentNode = rootNode.get("argument");
            if (argumentNode == null) {
                log.error("Argument is not specified in the request");
                return;
            }
            String command = commandNode.asText();
            String argument = argumentNode.asText();

            log.info("Command received: {} {}", command, argument);

            String response;
            switch (command) {
                case "zip":
                    response = executeZip(argument);
                    break;
                case "download":
                    response = executeDownload(argument);
                    break;
                case "sum":
                    response = executeSum(argument);
                    break;
                default:
                    throw new RuntimeException("Unknown command: " + command);
            }
            out.println(response);
        } catch (IOException e) {
            log.error("I/O error", e);
        }
    }

    private String executeDownload(String argument) {
        String[] urls = argument.split("\\|");
        //use IO executor here because it has many threads
        new DownloadCommand(urls, ioExecutor).download();
        return "ok";
    }

    private String executeSum(String argument) {
        //SUM command uses ForkJoinPool inside
        long sum = new SumCommand(Integer.parseInt(argument)).sum();
        log.info("SUM result: {}", sum);
        return "ok";
    }

    private String executeZip(String argument) {
        //use CPU executor here because compressing will probably consume a lot of CPU resources
        String result = new ZipCommand(argument, cpuExecutor).zip();
        return result;
    }
}
