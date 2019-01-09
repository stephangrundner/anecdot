package info.anecdot.thymeleaf;

import info.anecdot.model.Host;
import info.anecdot.model.Document;
import info.anecdot.model.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;

/**
 * @author Stephan Grundner
 */
@Component
@Scope("singleton")
public class Documents {

    private IExpressionContext context;

    @Autowired
    private DocumentService documentService;

    public IExpressionContext getContext() {
        return context;
    }

    public void setContext(IExpressionContext context) {
        this.context = context;
    }

//    private HttpServletRequest getRequest() {
//        return ((WebEngineContext) context).getRequest();
//    }

    public ResultList<Document> byUri(String uri, int offset, int limit) {
        Document document = (Document) context.getVariable("$item");
        Host host = document.getHost();

        Page<Document> page = documentService.findDocumentsByHostAndUriLike(host, uri, offset, limit);

        return new ResultList<>(page);
    }

    public ResultList<Document> byUri(String uri) {
        return byUri(uri, 0, Integer.MAX_VALUE);
    }


}
