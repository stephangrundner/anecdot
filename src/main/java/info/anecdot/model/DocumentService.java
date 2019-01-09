package info.anecdot.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class DocumentService {

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

//    public List<Article> findDocumentsByHostAndUriLike(Host host, String path) {
//        return findDocumentsByHostAndUriLike(host, path, 0, Integer.MAX_VALUE);
//    }

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

    public Map<String, Object> toMap(Fragment fragment) {
        Map<String, Object> model = new LinkedHashMap<>();

        model.put("$fragment", fragment);
        model.put("$text", fragment.getText());

        List<Object> children = new ArrayList<>();
        fragment.getSequences().forEach((name, sequence) -> {
            List<Object> values = sequence.getChildren().stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
            model.put(name, values);
            children.add(values);
        });

        model.put("$children", children);

//        Sequence sequence = fragment.getSequence();
//        if (sequence != null) {
//            model.put("$name", sequence.getName());
//            model.put("$ordinal", fragment.getOrdinal());
//        }

        return model;
    }
}
