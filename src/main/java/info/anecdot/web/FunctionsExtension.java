package info.anecdot.web;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stephan Grundner
 */
public class FunctionsExtension extends AbstractExtension {

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private class ItemsFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {

            SiteService siteService = applicationContext.getBean(SiteService.class);


            ItemService itemService = applicationContext.getBean(ItemService.class);
//            Site site = siteService.findSiteByRequest(currentRequest());
            String host = currentRequest().getServerName();
            Stream<Item> stream = itemService.findItemsByHost(host).stream();

//            String uri = (String) args.get("uri");
//            if (StringUtils.hasText(uri)) {
//                stream = stream.filter(itemService.filterByUri(uri));
//            }
//
//            String tags = (String) args.get("tags");
//            if (StringUtils.hasText(tags)) {
//                stream = stream.filter(itemService.filterByTags(tags.split(",")));
//            }
//
//            Object limit = args.get("limit");
//            if (limit instanceof Number) {
//                stream = stream.limit((long) limit);
//            }

//            String sort = (String) args.getOrDefault("sort", "date");
//
//            String order = (String) args.get("order");
//            if (StringUtils.hasText(order)) {
//                switch (order.toLowerCase()) {
//                    case "asc":
//                        stream = stream.sorted(itemService.sortedByDate(false));
//                        break;
//                    case "desc":
//                        stream = stream.sorted(itemService.sortedByDate(true));
//                        break;
//                    default:
//                        throw new RuntimeException("Unexpectd order: " + order);
//                }
//            }

            return stream.map(itemService::toMap).collect(Collectors.toList());
        }

        @Override
        public List<String> getArgumentNames() {
            return Arrays.asList("uri", "tags", "limit", "order", "sort");
        }
    }

    private class EvalFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext evaluationContext, int lineNumber) {
            ExpressionParser expressionParser = new SpelExpressionParser();
            Expression expression = expressionParser.parseExpression(args.get("expr").toString());
            org.springframework.expression.EvaluationContext expressionEvaluationContext = new StandardEvaluationContext();
            Object model = evaluationContext.getVariable("page");
            ((StandardEvaluationContext) expressionEvaluationContext).setRootObject(model);
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            SiteService siteService = applicationContext.getBean(SiteService.class);
            Site site = siteService.findSiteByRequest(servletRequestAttributes.getRequest());
            expressionEvaluationContext.setVariable("site", site);
            ((StandardEvaluationContext) expressionEvaluationContext).setBeanResolver(((context, beanName) -> {
                return applicationContext.getBean(beanName);
            }));

            return expression.getValue(expressionEvaluationContext);
        }

        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("expr");
        }
    }

    private final ApplicationContext applicationContext;

    @Override
    public Map<String, Function> getFunctions() {
        Map<String, Function> functions = new HashMap<>();
        functions.put("items", new ItemsFunction());
        functions.put("eval", new EvalFunction());

        return functions;
    }

    public FunctionsExtension(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
