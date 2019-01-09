package info.anecdot.model;

import info.anecdot.io.ItemLoader;
import info.anecdot.io.PathObserver;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Service
public class HostService {

    private static final Logger LOG = LoggerFactory.getLogger(HostService.class);

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private ItemLoader itemLoader;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private Environment environment;

    public String resolveHostName(HttpServletRequest request) {
        String hostName = Collections.list(request.getHeaderNames()).stream()
                .filter(it -> "X-Real-IP".equalsIgnoreCase(it)
                        || "X-Forwarded-For".equalsIgnoreCase(it)
                        || "X-Forwarded-Proto".equalsIgnoreCase(it))
                .flatMap(it -> Collections.list(request.getHeaders(it)).stream())
                .findFirst().orElse(null);

        if (StringUtils.isEmpty(hostName)) {
            hostName = request.getLocalName();
            if (StringUtils.isEmpty(hostName)) {
                hostName = request.getServerName();
            }
        }

        return hostName;
    }

    public List<Host> findAllHosts() {
        return hostRepository.findAll();
    }

    public Host findHostByRequest(HttpServletRequest request) {
        String name = resolveHostName(request);
        return hostRepository.findByNamesContaining(name);
    }

    public void saveHost(Host host) {
        hostRepository.saveAndFlush(host);
    }

    private String toUri(Host host, Path path) {
        Path base = host.getDirectory();
        Path content = base.resolve("content");
        Path filename = content.relativize(path);
        String extension = FilenameUtils.getExtension(filename.toString());
        String uri = "/" + org.apache.commons.lang.StringUtils.removeEnd(filename.toString(), "." + extension);

        return uri;
    }

    private boolean accept(Path path) {
        try {
            String fileName = path.getFileName().toString();
            String extension = FilenameUtils.getExtension(fileName);
            return !fileName.startsWith(".") && "xml".equals(extension);
//            TODO Also check if file is not ignored by .pageignore file!

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void reload(Host host, Path file) {
        try {
            if (!accept(file)) {
                LOG.info("Ignoring {}", file);
                return;
            }

            String uri = toUri(host, file);

            Document document = documentService.findDocumentByHostAndUri(host, uri);
            if (document == null) {
                document = itemLoader.loadPage(file);
                document.setHost(host);
                document.setUri(uri);
            } else {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                LocalDateTime modified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                if (!modified.isEqual(document.getModified())) {
                    documentService.deleteDocument(document);
                    document = itemLoader.loadPage(file);
                    document.setHost(host);
                    document.setUri(uri);
                }
            }

            documentService.saveDocument(document);
            LOG.info("Reloaded page for file {}", file);

            String url = "http://" + host.getName();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            url += uri;

            try {
//                new Crawler().crawl(url);
            } catch (Exception e) {
                LOG.error("Error while crawling {}", url);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void observe(Host host) throws IOException {
        PathObserver observer = new PathObserver() {
            @Override
            protected void visited(Path file) {
                reload(host, file);
            }

            @Override
            protected void created(Path file) {
                visited(file);
            }

            @Override
            protected void modified(Path file) {
                visited(file);
            }

            @Override
            protected void deleted(Path path, boolean file) {
                if (!accept(path)) {
                    LOG.info("Ignoring {}", file);
                    return;
                }

                String uri = toUri(host, path);

                if (!file) {
                    List<Document> documents = documentService.findDocumentsByHostAndUriStartingWith(host, uri);

                    LOG.info("Deleted dir: {}", path);
                } else {
                    LOG.info("Deleted file: {}", path);
                }
            }
        };

        Path content = host.getDirectory().resolve("content");
        observer.start(content);
    }
}
