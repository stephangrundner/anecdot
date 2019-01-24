package info.anecdot.content;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    public static final String ITEM_BY_URI_CACHE = "itemByUri";
    public static final String ITEMS_BY_TAGS_CACHE = "itemsByTags";

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemService self;

    private AntPathMatcher pathMatcher;

    public AntPathMatcher getPathMatcher() {
        if (pathMatcher == null) {
            pathMatcher = new AntPathMatcher();
        }

        return pathMatcher;
    }

    public Predicate<? super Item> filterByUri(String uri) {
        return it -> getPathMatcher().match(uri, it.getUri());
    }

    @Cacheable(cacheNames = {ITEM_BY_URI_CACHE}, key = "#root.args[1]")
    public Item findItemBySiteAndUri(Site site, String uri) {
        if (uri.equals("/")) {
            return findItemBySiteAndUri(site, site.getHome());
        }

        return site.getItems().stream()
                .filter(filterByUri(uri))
                .findFirst().orElse(null);
    }

    private Site getSiteForRequest(HttpServletRequest request) {
        Site site = siteService.findSiteByRequest(request);
        if (site == null) {
            throw new RuntimeException("No site available");
        }

        return site;
    }

    public Item findItemByRequestAndUri(HttpServletRequest request, String uri) {
        Site site = getSiteForRequest(request);
        return self.findItemBySiteAndUri(site, uri);
    }

    public Predicate<? super Item> filterByTags(Collection<String> tags) {
        return it -> it.getTags().containsAll(tags);
    }

    public Comparator<? super Item> sortedByDate(boolean descent) {
        Comparator<? super Item> comparator = Comparator.comparing(Item::getDate, Comparator.nullsLast(Comparator.reverseOrder()));
        if (descent) {
            return comparator.reversed();
        }

        return comparator;
    }

    public Predicate<? super Item> filterByTags(String... tags) {
        return filterByTags(Arrays.asList(tags));
    }

    @Cacheable(cacheNames = {ITEMS_BY_TAGS_CACHE}, key = "#root.args[1]")
    public Set<Item> findItemsBySiteAndTags(Site site, Collection<String> tags) {
        Set<Item> items = site.getItems().stream()
                .filter(filterByTags(tags))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Collections.unmodifiableSet(items);
    }

    public Set<Item> findItemsByRequestAndTags(HttpServletRequest request, Collection<String> tags) {
        Site site = getSiteForRequest(request);
        return self.findItemsBySiteAndTags(site, tags);
    }

    private <K> Map<K, Object> createMap() {
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Payload payload, Map<String, Object> parent) {
        Map<String, Object> map = createMap();

        map.put("#payload", payload);
        map.put("#name", Optional.ofNullable(payload.getOwner()).map(Payload.Sequence::getName).orElse(null));
        map.put("#value", payload instanceof Text ? ((Text) payload).getValue() : null);
        if (payload instanceof Item) {
            map.put("#tags", ((Item) payload).getTags());
        }
        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        List<Object> children = new ArrayList<>();

        if (payload instanceof Fragment) {
            for (Payload.Sequence sequence : ((Fragment) payload).getSequences()) {
                Map<Object, Object> values = createMap();
                int i = 0;
                String childName = sequence.getName();

                for (Payload child : sequence.getPayloads()) {
                    Map<String, Object> childMap = toMap(child, map);
                    if (i == 0) {
                        values.putAll(childMap);
                    }
                    values.put(Integer.toString(i++), childMap);
                }

                map.put(childName, values);
                children.add(values);
            }
        }

        map.put("#children", children);

        return map;
    }

    public Map<String, Object> toMap(Payload payload) {
        return toMap(payload, Collections.emptyMap());
    }
}
