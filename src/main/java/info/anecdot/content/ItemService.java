package info.anecdot.content;

import org.apache.commons.collections4.map.LazyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    public static final String ITEM_BY_URI_CACHE = "itemByUri";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemService self;

    private UrlPathHelper pathHelper;

    public UrlPathHelper getPathHelper() {
        if (pathHelper == null) {
            pathHelper = new UrlPathHelper();
        }

        return pathHelper;
    }

//    private static HttpServletRequest currentRequest() {
//        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//    }
//
//    private static Site currentSite() {
//        HttpServletRequest request = currentRequest();
//        ApplicationContext applicationContext = WebApplicationContextUtils
//                .findWebApplicationContext(request.getServletContext());
//        SiteService siteService = applicationContext.getBean(SiteService.class);
//        return siteService.findSiteByRequest(request);
//    }

    @Cacheable(cacheNames = {ITEM_BY_URI_CACHE}, key = "#root.args[1]")
    public Item findItemBySiteAndUri(Site site, String uri) {
        if (uri.equals("/")) {
            return findItemBySiteAndUri(site, site.getHome());
        }

        return site.getPages().stream()
                .filter(it -> uri.equals(it.getUri()))
                .findFirst().orElse(null);
    }

    public Item findItemByRequestAndUri(HttpServletRequest request, String uri) {
        Site site = siteService.findSiteByRequest(request);
        if (site == null) {
            throw new RuntimeException("No site available");
        }
        return self.findItemBySiteAndUri(site, uri);
    }

//    @Cacheable(cacheNames = {ITEM_BY_PATH_CACHE})
//    public Item findItemByUri(String path) {
//        Site site = currentSite();
//        return findItemBySiteAndUri(site, path);
//    }

    private <K> Map<K, Object> createMap() {
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Payload payload) {
        Map<String, Object> map = createMap();

        map.put("#name", Optional.ofNullable(payload.getOwner()).map(Payload.Sequence::getName).orElse(null));
        map.put("#value", payload instanceof Text ? ((Text) payload).getValue() : null);

        List<Object> children = new ArrayList<>();
        map.put("#children", children);

        if (payload instanceof Fragment) {
            for (Payload.Sequence sequence : ((Fragment) payload).getSequences()) {
                Map<Object, Object> values = createMap();
                int i = 0;
                String childName = sequence.getName();

                for (Payload child : sequence.getPayloads()) {
                    Map<String, Object> childMap = toMap(child);
                    if (i == 0) {
                        values.putAll(childMap);
                    }
                    values.put(Integer.toString(i++), childMap);
                }

                map.put(childName, values);
                children.add(values);
            }
        }

//        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
//            @Override
//            public String toString() {
//                Map<String, Object> decorated = decorated();
//                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
//            }
//        });

        return map;
    }
}
