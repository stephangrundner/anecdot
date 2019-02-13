package info.anecdot.content;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Stephan Grundner
 */
@Service
public class ContentService {

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




    //    @Cacheable(cacheNames = "items", key = "{#site.host, #uri}")
    public Item findItemBySiteAndUri(Site site, String uri) {
        if (uri.equals("/")) {
            uri = site.getHome();
        }

        Item item = site.getItem(uri);

        return item;
    }

    private boolean hasChildElements(Node node) {
        NodeList children = node.getChildNodes();
        if (children.getLength() == 0) {
            return false;
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }

        return false;
    }

    private boolean isRootNode(Node node) {
        Node parentNode = node.getParentNode();
        return parentNode != null && parentNode.getNodeType() == Node.DOCUMENT_NODE;
    }

    private Payload fromNode(Node node) {
        Payload payload;

        if (isRootNode(node)) {
            Item item = new Item();
            item.setType(node.getNodeName());

            payload = item;
        } else {
            payload = new Payload();
        }

        if (hasChildElements(node)) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeName().startsWith("#")) {
                    continue;
                }

                String childName = childNode.getNodeName();
                List<Payload> sequence = payload.getSequences().get(childName);
                if (sequence == null) {
                    sequence = new ArrayList<>();
                    payload.getSequences().put(childName, sequence);
                }
                sequence.add(fromNode(childNode));
            }
        } else {
            payload.setText(node.getTextContent());
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                payload.getAttributes().put(attribute.getName(), attribute.getValue());
            }
        }

        return payload;
    }

    @CacheEvict(cacheNames = "items", key = "{#site.host, #site.toUri(#file)}")
    public Item loadItem(Site site, Path file) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(inputStream);
            Item item = (Item) fromNode(document.getDocumentElement());

            item.setSite(site);

            String uri = site.toUri(file);
            item.setUri(uri);
            site.addItem(item);

            return item;
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private <K> Map<K, Object> createMap() {
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Payload payload, String name, Map<String, Object> parent) {
        Map<String, Object> map = createMap();

        map.put("#payload", payload);
        map.put("#name", name);
//        map.put("#name", Optional.ofNullable(payload.getOwner()).map(Payload.Sequence::getName).orElse(null));
        map.put("#value", payload.getText());
//        if (payload instanceof Item) {
//            map.put("#tags", ((Item) payload).getTags());
//        }
        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        List<Object> children = new ArrayList<>();

        payload.getSequences().forEach((x, sequence) -> {
            Map<Object, Object> values = createMap();
            int i = 0;

            for (Payload child : sequence) {
                Map<String, Object> childMap = toMap(child, x, map);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(Integer.toString(i++), childMap);
            }

            map.put(x, values);
            children.add(values);
        });

        map.put("#children", children);

        return map;
    }

    public Map<String, Object> toMap(Payload payload) {
        return toMap(payload, null, Collections.emptyMap());
    }


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

            Locale locale = propertyResolver.getProperty(prefix + ".locale", Locale.class);
            site.setLocale(locale);

            Observer observer = beanFactory.getBean(Observer.class, site);
            site.setObserver(observer);

            sites.add(site);
            cache.put(host, site);

            executorService.execute(observer);
        }
    }
}
