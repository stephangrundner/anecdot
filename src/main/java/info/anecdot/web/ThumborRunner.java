package info.anecdot.web;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.UrlResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Paths;

/**
 * @author Stephan Grundner
 * <p>
 * https://github.com/thumbor/thumbor/wiki/Running
 * https://thumbor.readthedocs.io/en/latest/index.html
 * https://thumbor.readthedocs.io/en/latest/image_loader.html
 * https://thumbor.readthedocs.io/en/latest/configuration.html
 */
public class ThumborRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ThumborRunner.class);

    private static volatile boolean instantiated = false;{
        if (instantiated) {
            throw new IllegalStateException();
        }
        instantiated = true;
    }

    private Process process;
    private Integer exitValue;

    private int port = 8889;
    private String loggingLevel = "DEBUG";

    private OutputStream outputStream = System.out;

    public Integer getExitValue() {
        return exitValue;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        checkIfRunning();

        this.port = port;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        checkIfRunning();

        this.loggingLevel = loggingLevel;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream must not be null");
        }

        this.outputStream = outputStream;
    }

    private void checkIfRunning() {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("Thumbor already running");
        }
    }

    protected String buildUrl(HttpServletRequest request) {
        String size = request.getParameter("size");
        String requestUrl = request.getRequestURL().toString();
        requestUrl = requestUrl.replaceAll("https://", "http://");
        String url = String.format("http://localhost:%d/%s/%s/%s",
                port,
                "unsafe",
                size,
                requestUrl);

        return url;
    }

    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = buildUrl(request);

        long start = System.currentTimeMillis();
        UrlResource resource = new UrlResource(url);
        try (InputStream inputStream = resource.getInputStream();
             OutputStream outputStream = response.getOutputStream()) {

            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long stop = System.currentTimeMillis() - start;

        LOG.info("Processing took {} ms", stop);
    }

    @Override
    public synchronized void run(ApplicationArguments args) throws Exception {
        checkIfRunning();

        File configFile = File.createTempFile("thumbor", ".conf");
        configFile.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(configFile);
             PrintStream printer = new PrintStream(outputStream)) {

            printer.println("LOADER = 'thumbor.loaders.http_loader'");
            printer.printf("STORAGE='%s'\n", "thumbor.storages.file_storage");
//            printer.printf("STORAGE_EXPIRATION_SECONDS=%d\n", 60 * 60);
//            printer.printf("FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE='%s'\n", "thumbor.result_storages.file_storage");
//            printer.printf("RESULT_STORAGE_FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE_STORES_UNSAFE=%s\n", "True");;
        }

        ProcessBuilder builder = new ProcessBuilder()
                .command("thumbor",
                        "-p", Integer.toString(port),
                        "-l", loggingLevel,
                        "-c", configFile.toString());

        File directory = Paths.get(".").toRealPath().toFile();
        builder.directory(directory);
        builder.redirectErrorStream(true);

        process = builder.start();

        try (InputStreamReader reader = new InputStreamReader(process.getInputStream());
             BufferedReader buffer = new BufferedReader(reader);
             PrintStream printer = new PrintStream(outputStream)) {

            String line;
            while (process.isAlive() && (line = buffer.readLine()) != null) {
                printer.println(line);
                LOG.debug(line);
            }
        }

        exitValue = process.waitFor();
    }
}