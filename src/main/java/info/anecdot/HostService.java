package info.anecdot;

import info.anecdot.io.ItemLoader;
import info.anecdot.io.PathObserver;
import info.anecdot.model.Host;
import info.anecdot.model.HostRepository;
import info.anecdot.model.Item;
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
    private ItemService itemService;

    @Autowired
    private Environment environment;

    private String resolveHostName(HttpServletRequest request) {
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

    @Deprecated
    public Host resolveHost(HttpServletRequest request) {
        String name = resolveHostName(request);
        return hostRepository.findByName(name);
    }

    public List<Host> findAllHosts() {
        return hostRepository.findAll();
    }

    public Host findHostByDirectory(Path directory) {
        return hostRepository.findByDirectory(directory);
    }

    public Host findHostByRequest(HttpServletRequest request) {
        String name = resolveHostName(request);
        return hostRepository.findByName(name);
    }

    protected void saveHost(Host host) {
        hostRepository.saveAndFlush(host);
    }

    private String toUri(Host host, Path path) {
        Path filename = host.getDirectory().relativize(path);
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

            Item item = itemService.findItemByHostAndUri(host, uri);
            if (item == null) {
                item = itemLoader.loadPage(file);
                item.setHost(host);
                item.setUri(uri);
            } else {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                LocalDateTime modified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                if (!modified.isEqual(item.getModified())) {
                    itemService.deletePage(item);
                    item = itemLoader.loadPage(file);
                    item.setHost(host);
                    item.setUri(uri);
                }
            }

            itemService.savePage(item);
            LOG.info("Reloaded page for file {}", file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void observe(Host host) throws IOException {
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
                    List<Item> items = itemService.findItemsByHostAndUriStartingWith(host, uri);

                    LOG.info("Deleted dir: {}", path);
                } else {
                    LOG.info("Deleted file: {}", path);
                }
            }
        };

        observer.start(host.getDirectory());
    }
}
