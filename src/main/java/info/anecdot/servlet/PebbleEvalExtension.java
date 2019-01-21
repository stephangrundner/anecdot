package info.anecdot.servlet;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Stephan Grundner
 */
@Component
public class PebbleEvalExtension extends AbstractExtension {

    private static final String EXPR_ARGUMENT_NAME = "expr";

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Map<String, Function> getFunctions() {
        return Collections.singletonMap("eval", new Function() {
            @Override
            public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext evaluationContext, int lineNumber) {
                ExpressionParser expressionParser = new SpelExpressionParser();
                Expression expression = expressionParser.parseExpression(args.get(EXPR_ARGUMENT_NAME).toString());
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
                return Collections.singletonList(EXPR_ARGUMENT_NAME);
            }
        });
    }
}
