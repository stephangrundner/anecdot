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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

//    private final Set<Site> sites = new LinkedHashSet<>();

    @Autowired
    private SiteRepository siteRepository;

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
//        return sites.stream()
//                .filter(it -> it.getAliases().contains(host))
//                .findFirst().orElse(null);
        return siteRepository.findByHost(host);
    }

    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    @Deprecated
    private void reloadSiteXml()  {
        Path file = Paths.get("./grundner/.site.xml");
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try (InputStream inputStream = Files.newInputStream(file)) {
                Document document = parser.parse(inputStream);
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList restrictions = (NodeList) xPath.evaluate(
                        "/site/security/restrictions/restriction",
                        document, XPathConstants.NODESET);
                for (int i = 0; i < restrictions.getLength(); i++) {
                    Element element = (Element) restrictions.item(i);
                    String path = (String) xPath.evaluate("path/text()", element, XPathConstants.STRING);
                    NodeList roles = (NodeList) xPath.evaluate("roles/role",
                            element, XPathConstants.NODESET);
                    for (int j = 0; j < roles.getLength(); j++) {
                        Element role = (Element) roles.item(j);
                        String pattern = role.getTextContent();

                        "".toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadProperties(Path file) throws IOException {
        observationService.stopAll();

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

            String host = propertyResolver.getProperty(prefix + ".host");
            site.setHost(host);

            List<String> names = getProperties(propertyResolver, prefix + ".aliases");
            site.getAliases().addAll(names);

            String content = propertyResolver.getProperty(prefix + ".base");
            if (StringUtils.hasText(content)) {
                site.setBase(Paths.get(content));
            }

            String theme = propertyResolver.getProperty(prefix + ".theme");
            if (StringUtils.hasText(theme)) {
                site.setTheme(Paths.get(theme));
            }

            site.setHome(propertyResolver.getProperty(prefix + ".home", "/home"));

            saveSite(site);
            observationService.start(site);
        }

        reloadSiteXml();
    }
}
