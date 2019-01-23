package info.anecdot.servlet;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Component
public class PebbleItemsExtension extends AbstractExtension {

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private class ItemsFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            return null;
        }

        @Override
        public List<String> getArgumentNames() {
            return Collections.emptyList();
        }
    }

    private class ItemFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            SiteService siteService = applicationContext.getBean(SiteService.class);
            Site site = siteService.findSiteByRequest(currentRequest());
            ItemService itemService = applicationContext.getBean(ItemService.class);
            String uri = (String) args.get("uri");
            Item item = itemService.findItemBySiteAndUri(site, uri);

            return item != null ? itemService.toMap(item) : Collections.emptyMap();
        }

        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("uri");
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Map<String, Function> getFunctions() {
        Map<String, Function> functions = new HashMap<>();
        functions.put("items", new ItemsFunction());
        functions.put("item", new ItemFunction());

        return functions;
    }
}
