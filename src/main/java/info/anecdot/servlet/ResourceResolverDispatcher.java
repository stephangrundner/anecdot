package info.anecdot.servlet;

import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stephan Grundner
 */
public class ResourceResolverDispatcher extends AbstractResourceResolver implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private boolean isHidden(Site site, String requestPath) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        Set<String> hidden = site.getHidden();
        return hidden.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        SiteService siteService = applicationContext.getBean(SiteService.class);
        Site site = siteService.findSiteByRequest(request);

        if (isHidden(site, requestPath)) {
            return null;
        }

        Path directory = site.getContent();
        locations =
                Stream.concat(Stream.of("./")
                                .map(directory::resolve)
                                .map(it -> {
                                    String location = "file:" + it.toString();
                                    if (!location.endsWith("/")) {
                                        location += "/";
                                    }

                                    return applicationContext.getResource(location);
                                }),
                        Stream.of("file:./themes/dark/")
                                .map(it -> applicationContext.getResource(it)))
                        .collect(Collectors.toList());

        Resource result = chain.resolveResource(request, requestPath, locations);

        return result;
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        throw new UnsupportedOperationException();
    }
}
