package info.anecdot.web;

import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Reader;
import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
public class PebbleLoaderDecorator implements Loader<String> {

    private final SiteService siteService;

    private final Loader<String> loader;

    @Override
    public Reader getReader(String templateName) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        Site site = siteService.findSiteByRequest(request);
        Path directory = site.getTheme();

        try {
            String prefix = directory.toRealPath().toString();
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }
            templateName = prefix + templateName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return loader.getReader(templateName);
    }

    @Override
    public void setCharset(String charset) {
        loader.setCharset(charset);
    }

    @Override
    public void setPrefix(String prefix) {
        loader.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        loader.setSuffix(suffix);
    }

    @Override
    public String resolveRelativePath(String relativePath, String anchorPath) {
        return loader.resolveRelativePath(relativePath, anchorPath);
    }

    @Override
    public String createCacheKey(String templateName) {
        return loader.createCacheKey(templateName);
    }

    public PebbleLoaderDecorator(SiteService siteService, Loader<String> loader) {
        this.siteService = siteService;
        this.loader = loader;
    }
}
