package info.anecdot.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class SiteService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteService.class);

    public static final String SITE_BY_HOST_CACHE = "siteByHost";

    private static List<String> getProperties(PropertyResolver propertyResolver, String key, List<String> defaultValues) {
        class StringArrayList extends ArrayList<String> {
            private StringArrayList(Collection<? extends String> c) {
                super(c);
            }

            public StringArrayList() {
            }
        }

        return propertyResolver.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
    }

    public static List<String> getProperties(PropertyResolver propertyResolver, String key) {
        return getProperties(propertyResolver, key, Collections.emptyList());
    }

    private final Set<Site> sites = new LinkedHashSet<>();

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObservationService observationService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SiteService self;

    public String resolveHostName(HttpServletRequest request) {
        String hostName = Collections.list(request.getHeaderNames()).stream()
                .filter(it -> "X-Real-IP".equalsIgnoreCase(it)
                        || "X-Forwarded-For".equalsIgnoreCase(it)
                        || "X-Forwarded-Proto".equalsIgnoreCase(it))
                .flatMap(it -> Collections.list(request.getHeaders(it)).stream())
                .findFirst().orElse(null);

        if (StringUtils.isEmpty(hostName)) {
            hostName = request.getLocalName();
            if (StringUtils.isEmpty(hostName)) {
                hostName = request.getServerName();
            }
        }

        return hostName;
    }

    public Site findSiteByRequest(HttpServletRequest request) {
        String host = resolveHostName(request);
        return self.findSiteByHost(host);
    }

    @Cacheable(cacheNames = {SITE_BY_HOST_CACHE})
    public Site findSiteByHost(String host) {
        return sites.stream()
                .filter(it -> it.getHosts().contains(host))
                .findFirst().orElse(null);
    }

    public void reloadProperties(Path file) throws IOException {
        observationService.stopAll();
        sites.clear();

        Cache siteByHostCache = cacheManager.getCache(SiteService.SITE_BY_HOST_CACHE);
        siteByHostCache.clear();

        Cache itemByUriCache = cacheManager.getCache(ItemService.ITEM_BY_URI_CACHE);
        itemByUriCache.clear();

        Resource resource = resourceLoader.getResource("file:" + file.toRealPath());
        Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        MutablePropertySources sources = new MutablePropertySources();
        PropertiesPropertySource source = new PropertiesPropertySource("sites", properties);
        sources.addFirst(source);
        PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(sources);

        List<String> keys = getProperties(propertyResolver, "anecdot.sites");
        for (String key : keys) {
            Site site = new Site();
            String prefix = String.format("anecdot.site.%s", key);

            List<String> names = getProperties(propertyResolver, prefix + ".hosts");
            site.getHosts().addAll(names);

            String content = propertyResolver.getProperty(prefix + ".base");
            if (StringUtils.hasText(content)) {
                site.setBase(Paths.get(content));
            }

            String theme = propertyResolver.getProperty(prefix + ".theme");
            if (StringUtils.hasText(theme)) {
                site.setTheme(Paths.get(theme));
            }

            site.setHome(propertyResolver.getProperty(prefix + ".home", "/home"));

            sites.add(site);
            observationService.start(site);
        }
    }
}
