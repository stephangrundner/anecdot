package info.anecdot.content;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * @author Stephan Grundner
 */
@Service
public class SiteService {

    private static List<String> getProperties(PropertyResolver propertyResolver, String key, List<String> defaultValues) {
        class StringArrayList extends ArrayList<String> {
            private StringArrayList(Collection<? extends String> c) {
                super(c);
            }

            public StringArrayList() { }
        }

        return propertyResolver.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
    }

    public static List<String> getProperties(PropertyResolver propertyResolver, String key) {
        return getProperties(propertyResolver, key, Collections.emptyList());
    }

    @Autowired
    private ListableBeanFactory beanFactory;

    private final Set<Site> sites = new LinkedHashSet<>();
    private final Set<SiteObserver> observers = new LinkedHashSet<>();

    private SiteObserver findObserverBySite(Site site) {
        return observers.stream()
                .filter(it -> it.getSite().equals(site))
                .findFirst()
                .orElse(null);
    }

    public String toUri(Site site, Path file) {
        Path base = site.getBase();
        String uri = base.relativize(file).toString();
        uri = FilenameUtils.removeExtension(uri);
        if (!StringUtils.startsWithIgnoreCase(uri, "/")) {
            uri = "/" + uri;
        }

        return uri;
    }

    private SiteObserver observe(Site site) throws IOException {
        SiteObserver observer = findObserverBySite(site);
        if (observer != null) {
            throw new IllegalStateException("Observation already running for site " + site);
        }

        observer = beanFactory.getBean(SiteObserver.class, site);
        observers.add(observer);

        Executors.newSingleThreadExecutor().execute(observer);

        return observer;
    }

    public Site findSiteByHost(String host) {
        return sites.stream()
                .filter(it -> it.getHost().equals(host))
                .findFirst()
                .orElse(null);
    }

    public Site findSiteByRequest(HttpServletRequest request) {
        String host = request.getServerName();
        return findSiteByHost(host);
    }

    public void reloadSites(PropertyResolver propertyResolver) throws IOException {
        sites.clear();

        List<String> keys = getProperties(propertyResolver, "anecdot.sites");
        for (String key : keys) {

            String prefix = String.format("anecdot.site.%s", key);

            String host = propertyResolver.getProperty(prefix + ".host");

            Site site = findSiteByHost(host);
            if (site == null) {
                site = new Site();
            }

            site.setHost(host);

//            List<String> names = getProperties(propertyResolver, prefix + ".aliases");
//            site.getAliases().addAll(names);

            String content = propertyResolver.getProperty(prefix + ".base");
            if (StringUtils.hasText(content)) {
                site.setBase(Paths.get(content));
            }

            String theme = propertyResolver.getProperty(prefix + ".theme");
            if (StringUtils.hasText(theme)) {
                site.setTheme(Paths.get(theme));
            }

            String home = propertyResolver.getProperty(prefix + ".home", "/home");
            site.setHome(home);

            sites.add(site);

            observe(site);
        }
    }
}
