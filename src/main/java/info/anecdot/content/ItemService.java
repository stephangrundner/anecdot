package info.anecdot.content;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

    private UrlPathHelper pathHelper;

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemRepository itemRepository;

    public UrlPathHelper getPathHelper() {
        if (pathHelper == null) {
            pathHelper = new UrlPathHelper();
        }

        return pathHelper;
    }

    public void setPathHelper(UrlPathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    public Item findPageById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public Item findItemBySiteAndUri(Site site, String uri) {
        return itemRepository.findBySiteAndUri(site, uri);
    }

    public Item findPageBySiteAndUri(Site site, String uri) {
        return itemRepository.findBySiteAndUriAndPageIsTrue(site, uri);
    }

    public List<Item> findPagesBySiteAndUriStartingWith(Site site, String path) {
        return itemRepository.findBySiteAndUriStartingWithAndPageIsTrue(site, path);
    }

//    public Slice<Page> findPagesByHostAndUriLike(Site site, String path, int offset, int limit) {
//        return pageRepository.findBySiteAndUriLike(site, path, PageRequest.of(offset, limit));
//    }

    public Item findPageByRequest(HttpServletRequest request) {
        Site site = siteService.findSiteByRequest(request);
        UrlPathHelper pathHelper = getPathHelper();

        String uri = pathHelper.getRequestUri(request);
        if (uri.equals("/")) {
            uri = site.getHome();
        }

        return findPageBySiteAndUri(site, uri);
    }

    public void savePage(Item item) {
        itemRepository.saveAndFlush(item);
    }

    public void deletePage(Item item) {
        itemRepository.delete(item);
    }

    private <K> Map<K, Object> createMap(){
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    private Map<String, Object> toMap(Fragment fragment, String fragmentName, Map<String, Object> parent, int index) {
        Map<String, Object> map = createMap();

        map.put("#index", index);
        map.put("#name", fragmentName);
        map.put("#value", fragment.getText());
        map.put("#attributes", fragment.getAttributes());

        List<Object> children = new ArrayList<>();
        map.put("#children", children);

        for (Map.Entry<String, Fragment> entry : fragment.getChildren().entrySet()) {
            Map<Object, Object> values = createMap();
            int i = 0;
            String childName = entry.getKey();
            Fragment child = entry.getValue();
            while (child != null) {
                Map<String, Object> childMap = toMap(child, childName, map, i);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(Integer.toString(i++), childMap);
                child = child.getNext();
            }

            map.put(childName, values);
            children.add(values);
        }

        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        return map;
    }

    private Map<String, Object> toMap(Fragment fragment, String fragmentName) {
        return toMap(fragment, fragmentName, Collections.emptyMap(), 0);
    }

    public Map<String, Object> toMap(Item item) {
        return toMap(item, item.getType());
    }
}
