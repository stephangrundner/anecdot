package info.anecdot.web;

import info.anecdot.content.Site;
import info.anecdot.content.ContentService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class ResourceResolverDispatcher extends AbstractResourceResolver implements ApplicationContextAware {

    public static final String THEME_URL_PATH_PREFIX = "theme/";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private Resource toFileResource(Path path) {
        String location = "file:" + path.toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        return applicationContext.getResource(location);
    }

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

        String name = FilenameUtils.getName(requestPath);
        if (StringUtils.startsWithIgnoreCase(name, ".")) {
            return null;
        }

        String host = request.getServerName();
        ContentService contentService = applicationContext.getBean(ContentService.class);
        Site site = contentService.findSiteByHost(host);

        String size = request.getParameter("size");
        if (size != null) {
            String imageBasePath = site.getBase().toString();

            if (imageBasePath.startsWith("./")) {
                imageBasePath = imageBasePath.substring(2);
            }

            if (!imageBasePath.endsWith("/")) {
                imageBasePath += "/";
            }

            Environment environment = applicationContext.getEnvironment();
            Integer port = environment.getProperty("thumbor.port", Integer.class);
            String thumborUrl = String.format("http://localhost:%d/%s/%s/%s",
                    port,
                    "unsafe",
                    size,
                    imageBasePath + requestPath);

            try {
                return new UrlResource(thumborUrl) {
                    @Override
                    public long lastModified() throws IOException {
                        return -1;
                    }

                    @Override
                    public synchronized long contentLength() throws IOException {
                        return -1;
                    }

                    @Override
                    public boolean isReadable() {
                        return true;
                    }

                    @Override
                    public Resource createRelative(String relativePath) throws MalformedURLException {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        if (requestPath.startsWith(THEME_URL_PATH_PREFIX)) {
            requestPath = requestPath.substring(THEME_URL_PATH_PREFIX.length(), requestPath.length());
            locations = Collections.singletonList(toFileResource(site.getTheme()));
        } else {
            locations = Collections.singletonList(toFileResource(site.getBase()));
        }

        return chain.resolveResource(request, requestPath, locations);
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        throw new UnsupportedOperationException();
    }
}
