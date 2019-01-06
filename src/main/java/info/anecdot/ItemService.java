package info.anecdot;

import info.anecdot.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    private UrlPathHelper pathHelper;

    @Autowired
    private HostService hostService;

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

    public Item findItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public Item findItemByHostAndUri(Host host, String uri) {
        return itemRepository.findByHostAndUri(host, uri);
    }

    public List<Item> findItemsByHostAndUriStartingWith(Host host, String path) {
        return itemRepository.findByHostAndUriStartingWith(host, path);
    }

    public Page<Item> findItemsByHostAndUriLike(Host host, String path, int offset, int limit) {
        return itemRepository.findByHostAndUriLike(host, path, PageRequest.of(offset, limit));
    }

//    public List<Article> findItemsByHostAndUriLike(Host host, String path) {
//        return findItemsByHostAndUriLike(host, path, 0, Integer.MAX_VALUE);
//    }

    public Item findItemByRequest(HttpServletRequest request) {
        Host host = hostService.findHostByRequest(request);
        UrlPathHelper pathHelper = getPathHelper();

        String uri = pathHelper.getRequestUri(request);
        if (uri.equals("/")) {
            uri = host.getHome();
        }

        return findItemByHostAndUri(host, uri);
    }

    public void savePage(Item item) {
        itemRepository.saveAndFlush(item);
    }

    public void deletePage(Item item) {
        itemRepository.delete(item);
    }

    public Map<String, Object> toMap(Payload payload) {
        Map<String, Object> model = new LinkedHashMap<>();

        if (payload instanceof Fragment) {
            Fragment fragment = (Fragment) payload;
            fragment.getSequences().forEach((name, sequence) -> {
                model.put(name, sequence.getPayloads().stream()
                        .map(this::toMap)
                        .collect(Collectors.toList()));
            });
        } else {
            Text text = (Text) payload;
            model.put("$text", text.getValue());
        }

        model.put("$payload", payload);

        return model;
    }
}
