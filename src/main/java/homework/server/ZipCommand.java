package homework.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ZipCommand {

    private static final Logger log = LoggerFactory.getLogger(ZipCommand.class);

    private String rootPath;

    private ExecutorService executorService;

    public ZipCommand(String rootPath, ExecutorService executorService) {
        this.rootPath = rootPath;
        this.executorService = executorService;
    }

    public String zip() {
        log.info("ZIP command started");
        try {
            List<Path> filesToArchive = Files.walk(Paths.get(rootPath))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            List<Callable<String>> callables = filesToArchive.stream()
                    .map(path -> (Callable<String>) () -> {
                        //fake command that doesn't actually compress files
                        log.info("Imagine we archived the file {}", path);
                        return path.toString();
                    })
                    .collect(Collectors.toList());

            List<String> zipFiles = new ArrayList<>();
            try {
                List<Future<String>> futures = executorService.invokeAll(callables, 30, TimeUnit.SECONDS);
                //accumulate tasks results
                for (Future<String> future : futures) {
                    zipFiles.add(future.get());
                }
                log.info("Zipped files: {}", zipFiles);
                return "ok";
            } catch (InterruptedException e) {
                log.error("Interrupted error", e);
            } catch (ExecutionException e) {
                log.error("Execution error", e);
            }
        } catch (IOException e) {
            log.error("I/O error on accessing the starting file");
        }
        return "failed";
    }
}
