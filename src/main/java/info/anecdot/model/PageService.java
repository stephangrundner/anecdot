package info.anecdot.model;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class PageService extends FragmentService {

    private static final Logger LOG = LoggerFactory.getLogger(PageService.class);

    private UrlPathHelper pathHelper;

    @Autowired
    private SiteService siteService;

    @Autowired
    private PageRepository pageRepository;

    public UrlPathHelper getPathHelper() {
        if (pathHelper == null) {
            pathHelper = new UrlPathHelper();
        }

        return pathHelper;
    }

    public void setPathHelper(UrlPathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    public Page findPageById(Long id) {
        return pageRepository.findById(id).orElse(null);
    }

    public Page findPageByHostAndUri(Site site, String uri) {
        return pageRepository.findBySiteAndUri(site, uri);
    }

    public List<Page> findPagesByHostAndUriStartingWith(Site site, String path) {
        return pageRepository.findBySiteAndUriStartingWith(site, path);
    }

    public Slice<Page> findPagesByHostAndUriLike(Site site, String path, int offset, int limit) {
        return pageRepository.findBySiteAndUriLike(site, path, PageRequest.of(offset, limit));
    }

    public Page findPageByRequest(HttpServletRequest request) {
        Site site = siteService.findSiteByRequest(request);
        UrlPathHelper pathHelper = getPathHelper();

        String uri = pathHelper.getRequestUri(request);
        if (uri.equals("/")) {
            uri = site.getHome();
        }

        return findPageByHostAndUri(site, uri);
    }

    public void savePage(Page page) {
        pageRepository.saveAndFlush(page);
    }

    public void deletePage(Page page) {
        pageRepository.delete(page);
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

    public Map<String, Object> toMap(Page page) {
        return toMap(page, page.getType());
    }
}
