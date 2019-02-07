package info.anecdot.web;

import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class ResourceResolverDispatcher extends AbstractResourceResolver implements ApplicationContextAware {

    private static final String THEME_URL_PATH_PREFIX = "theme/";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private String ensureTrailingSlash(String uri) {
        if (!uri.endsWith("/")) {
            return uri + "/";
        }

        return uri;
    }

    private Resource toFileResource(Path path) {
        String location = "file:" + path.toString();
        return applicationContext.getResource(ensureTrailingSlash(location));
    }

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        SiteService siteService = applicationContext.getBean(SiteService.class);
        Site site = siteService.findSiteByRequest(request);

        String name = FilenameUtils.getName(requestPath);
        if (StringUtils.startsWithIgnoreCase(name, ".")) {
            return null;
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
