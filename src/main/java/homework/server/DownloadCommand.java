package homework.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class DownloadCommand {

    private static final Logger log = LoggerFactory.getLogger(DownloadCommand.class);

    private String[] urls;
    private ExecutorService executor;

    public DownloadCommand(String[] urls, ExecutorService executor) {
        this.urls = urls;
        this.executor = executor;
    }

    public String download() {
        log.info("DOWNLOAD command started");
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(executor)
                .build();

        List<Callable<String>> tasks = new ArrayList<>();
        for (String url : urls) {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            tasks.add(() -> {
                log.debug("Trying to download {}", url);
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                String body = httpResponse.body();
                long checksum = getChecksum(body.getBytes(StandardCharsets.UTF_8));
                log.info("Site {}, checksum {}", url, checksum);
                return String.valueOf(checksum);
            });
        }

        List<String> combinedChecksums = new ArrayList<>();
        try {
            List<Future<String>> futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
            //accumulate tasks results
            for (Future<String> future : futures) {
                combinedChecksums.add(future.get());
            }
        } catch (InterruptedException e) {
            log.error("Error", e);
        } catch (ExecutionException e) {
            log.error("Execution error", e);
        }
        return String.join(" ", combinedChecksums);
    }

    private long getChecksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
}
