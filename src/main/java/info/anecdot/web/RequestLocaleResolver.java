package info.anecdot.web;

import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Stephan Grundner
 */
public class RequestLocaleResolver implements org.springframework.web.servlet.LocaleResolver {

    @Autowired
    private SiteService siteService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Site site = (Site) request.getAttribute(Site.class.getName());
        if (site == null) {
            String host = request.getServerName();
            site = siteService.findSiteByHost(host);
        }

        return Optional
                .ofNullable(site.getLocale())
                .orElse(Locale.getDefault());
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }
}
