package info.anecdot.web;

import info.anecdot.content.Site;
import info.anecdot.content.ContentService;
import info.anecdot.content.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Stephan Grundner
 */
public class RequestLocaleResolver implements org.springframework.web.servlet.LocaleResolver {

    @Autowired
    private ContentService contentService;

    @Autowired
    private SettingsService settingsService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        AtomicReference<Locale> localeHolder = new AtomicReference<>();
        String uri = request.getRequestURI();
        settingsService.eachSettingsForUri(uri, settings -> {
            localeHolder.set(settings.getLocale());
        });

        if (localeHolder.get() == null) {
            Site site = (Site) request.getAttribute(Site.class.getName());
            if (site == null) {
                String host = request.getServerName();
                site = contentService.findSiteByHost(host);
            }
            localeHolder.set(site.getLocale());
        }

        return Optional.ofNullable(localeHolder.get())
                .orElse(Locale.getDefault());
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException();
    }
}
