package info.anecdot.content;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private final Set<Observer> observers = new HashSet<>();

    private Observer findObserverByHost(String host) {
        return observers.stream()
                .filter(it -> host.equals(it.getSite().getHost()))
                .findFirst()
                .orElse(null);
    }

    @Cacheable(cacheNames = "observers", key = "#site.host")
    public Observer findObserverBySite(Site site) {
        return findObserverByHost(site.getHost());
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

    @CacheEvict(cacheNames = "observers", key = "#site.host")
    private Observer observe(Site site) throws IOException {
        Observer observer = findObserverBySite(site);
        if (observer != null) {
            throw new IllegalStateException("Observation already running for site " + site);
        }

        observer = beanFactory.getBean(Observer.class, site);
        observers.add(observer);

        Executors.newSingleThreadExecutor().execute(observer);

        return observer;
    }

    public Site findSiteByHost(String host) {
        Observer observer = findObserverByHost(host);
        if (observer != null) {

            return observer.getSite();
        }

        return null;
    }

    public Site findSiteByRequest(HttpServletRequest request) {
        String host = request.getServerName();
        return findSiteByHost(host);
    }

    public void reloadSites(PropertyResolver propertyResolver) throws IOException {
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

            observe(site);
        }
    }
}
