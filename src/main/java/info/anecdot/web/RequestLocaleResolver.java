package info.anecdot.web;

import info.anecdot.content.Site;
import info.anecdot.content.ContentService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Stephan Grundner
 */
public class RequestLocaleResolver implements org.springframework.web.servlet.LocaleResolver {

    @Autowired
    private ContentService contentService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Site site = (Site) request.getAttribute(Site.class.getName());
        if (site == null) {
            String host = request.getServerName();
            site = contentService.findSiteByHost(host);
        }

        return site.getLocale();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException();
    }
}
