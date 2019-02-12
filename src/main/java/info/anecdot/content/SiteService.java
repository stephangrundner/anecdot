package info.anecdot.content;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
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
    private CacheManager cacheManager;

    @Autowired
    private ListableBeanFactory beanFactory;

    private ExecutorService executorService;

    private final Set<Site> sites = new HashSet<>();

    public Site findSiteByHost(String host) {
        Cache cache = cacheManager.getCache("sites");
        Site site = cache.get(host, Site.class);
        if (site == null) {
            site = sites.stream()
                    .filter(it -> it.getHost().equals(host))
                    .findFirst().orElse(null);

            if (site != null) {
                cache.put(host, site);
            }
        }

        return site;
    }

    public void reloadSites(PropertyResolver propertyResolver) {
        sites.clear();

        List<String> keys = getProperties(propertyResolver, "anecdot.sites");

        if (executorService != null) {
            executorService.shutdown();
        }

        executorService = Executors.newFixedThreadPool(keys.size());

        for (String key : keys) {

            String prefix = String.format("anecdot.site.%s", key);

            String host = propertyResolver.getProperty(prefix + ".host");

            Cache cache = cacheManager.getCache("sites");
            cache.evict(host);

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

            Observer observer = beanFactory.getBean(Observer.class, site);
            site.setObserver(observer);

            sites.add(site);
            cache.put(host, site);

            executorService.execute(observer);
        }
    }
}
