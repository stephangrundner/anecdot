package info.anecdot.model;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class DocumentService extends FragmentService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    private UrlPathHelper pathHelper;

    @Autowired
    private HostService hostService;

    @Autowired
    private DocumentRepository documentRepository;

    public UrlPathHelper getPathHelper() {
        if (pathHelper == null) {
            pathHelper = new UrlPathHelper();
        }

        return pathHelper;
    }

    public void setPathHelper(UrlPathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    public Document findDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    public Document findDocumentByHostAndUri(Host host, String uri) {
        return documentRepository.findByHostAndUri(host, uri);
    }

    public List<Document> findDocumentsByHostAndUriStartingWith(Host host, String path) {
        return documentRepository.findByHostAndUriStartingWith(host, path);
    }

    public Page<Document> findDocumentsByHostAndUriLike(Host host, String path, int offset, int limit) {
        return documentRepository.findByHostAndUriLike(host, path, PageRequest.of(offset, limit));
    }

    public Document findDocumentByRequest(HttpServletRequest request) {
        Host host = hostService.findHostByRequest(request);
        UrlPathHelper pathHelper = getPathHelper();

        String uri = pathHelper.getRequestUri(request);
        if (uri.equals("/")) {
            uri = host.getHome();
        }

        return findDocumentByHostAndUri(host, uri);
    }

    public void saveDocument(Document document) {
        documentRepository.saveAndFlush(document);
    }

    public void deleteDocument(Document document) {
        documentRepository.delete(document);
    }

    private <K> Map<K, Object> createMap(){
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Fragment fragment, Map<String, Object> parentMap, int index) {
//        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> map = createMap();

        map.put("$index", index);
        map.put("$ordinal", Optional.ofNullable(fragment.getSequence()).map(Sequence::getOrdinal).orElse(0));
        map.put("$property", fragment.getPropertyPath());
        map.put("$value", fragment.getText());
        map.put("$text", new Object() {
            @Override
            public String toString() {
                LOG.warn("Dont use the $text property!");
                return (String) map.get("$value");
            }
        });
        map.put("$attributes", fragment.getAttributes());
        map.put("$fragment", fragment);

        List<Object> children = new ArrayList<>();

        for (Sequence sequence : fragment.getSequences()) {
//            Map<Object, Object> values = new LinkedHashMap<>();
            Map<Object, Object> values = createMap();
            int i = 0;
            for (Fragment child : sequence.getChildren()) {
                Map<String, Object> childMap = toMap(child, map, i);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(i++, childMap);
            }
            map.put(sequence.getName(), values);

            children.add(values);
        }

        map.put("$parent", new AbstractMapDecorator<String, Object>(parentMap) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        map.put("$children", children);

        return map;
    }

    public Map<String, Object> toMap(Fragment fragment) {
        return toMap(fragment, Collections.emptyMap(), 0);
    }
}
