package info.anecdot.thymeleaf;

import info.anecdot.model.Host;
import info.anecdot.model.Item;
import info.anecdot.model.ItemService;
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
public class Items {

    private IExpressionContext context;

    @Autowired
    private ItemService itemService;

    public IExpressionContext getContext() {
        return context;
    }

    public void setContext(IExpressionContext context) {
        this.context = context;
    }

//    private HttpServletRequest getRequest() {
//        return ((WebEngineContext) context).getRequest();
//    }

    public ResultList<Item> byUri(String uri, int offset, int limit) {
        Item item = (Item) context.getVariable("$item");
        Host host = item.getHost();

        Page<Item> page = itemService.findItemsByHostAndUriLike(host, uri, offset, limit);

        return new ResultList<>(page);
    }

    public ResultList<Item> byUri(String uri) {
        return byUri(uri, 0, Integer.MAX_VALUE);
    }


}
