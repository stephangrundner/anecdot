package info.anecdot.content;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Stephan Grundner
 */
@Service
public class SiteService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteService.class);

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageLoader pageLoader;

    @Autowired
    private PageService pageService;

    @Autowired
    private ApplicationContext applicationContext;

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

    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }

    public Site findSiteByRequest(HttpServletRequest request) {
        String name = resolveHostName(request);
        return siteRepository.findByHostsContaining(name);
    }

    public void saveSite(Site site) {
        siteRepository.saveAndFlush(site);
    }

    private String toUri(Site site, Path path) {
        Path directory = site.getContent();
        Path filename = directory.relativize(path);
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

    private void reload(Site site, Path file, boolean force) {
        try {
            if (!accept(file)) {
                LOG.info("Ignoring {}", file);
                return;
            }

            String uri = toUri(site, file);

            Page page = pageService.findPageBySiteAndUri(site, uri);
            if (page == null) {
                page = pageLoader.loadPage(file);
                page.setSite(site);
                page.setUri(uri);
            } else {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                LocalDateTime modified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                if (force || !modified.isEqual(page.getModified())) {
                    pageService.deletePage(page);
                    page = pageLoader.loadPage(file);
                    page.setSite(site);
                    page.setUri(uri);
                }
            }

            pageService.savePage(page);
            LOG.info("Reloaded page for file {}", file);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reload(Site site, Path file) {
        reload(site, file, false);
    }

    public void reloadProperties(Site site) throws IOException {
        Path directory = site.getContent();
        Path propertiesFile = directory.resolve(".properties");
        if (Files.exists(propertiesFile)) {
            Resource resource = applicationContext.getResource("file:" + propertiesFile.toRealPath());
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);

            if (properties.containsKey("theme")) {
                Path theme = Paths.get(properties.getProperty("theme"));
                site.setTheme(site.getContent().resolve(theme));
            }

//            properties.forEach((k, v) -> {
//                site.getProperties().put(k.toString(), v.toString());
//            });

            saveSite(site);
        }
    }

    private void reload(Site site) throws IOException {
        reloadProperties(site);
        saveSite(site);
        Files.walk(site.getContent())
                .forEach(file ->
                        reload(site, file, true));
    }

    public void observe(Site site) throws IOException {
        PathObserver observer = new PathObserver() {
            @Override
            protected void visited(Path file) {
                reload(site, file);
            }

            @Override
            protected void created(Path file) {
                visited(file);
            }

            @Override
            protected void modified(Path file) {

                if (".properties".equals(file.getFileName().toString())) {
                    try {
                        reload(site);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                visited(file);
            }

            @Override
            protected void deleted(Path path, boolean file) {
                if (!accept(path)) {
                    LOG.info("Ignoring {}", file);
                    return;
                }

                String uri = toUri(site, path);

                if (!file) {
                    List<Page> pages = pageService.findPagesBySiteAndUriStartingWith(site, uri);

                    LOG.info("Deleted dir: {}", path);
                } else {
                    LOG.info("Deleted file: {}", path);
                }
            }
        };

        observer.start(site.getContent());
    }
}
