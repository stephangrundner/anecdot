package info.anecdot;

import info.anecdot.model.Host;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class ResourceResolverDispatcher extends AbstractResourceResolver implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        HostService hostService = applicationContext.getBean(HostService.class);
        Host host = hostService.findHostByRequest(request);

        String directory = host.getDirectory().toString();
        if (!directory.endsWith("/")) {
            directory += "/";
        }

        String locationPattern = "file:" + directory + requestPath;
        try {
            locations = Arrays.asList(applicationContext.getResources(locationPattern));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Resource result = chain.resolveResource(request, requestPath, locations);

        return result;
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        throw new UnsupportedOperationException();
    }
}
