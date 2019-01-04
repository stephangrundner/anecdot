package info.anecdot.thymeleaf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Component
public class AnecdotDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

    private static final String PREFIX = "a";

    @Autowired
    private ApplicationContext applicationContext;

//    public ComponentReplaceProcessor createReplaceProcessor() {
//        return new ComponentReplaceProcessor(PREFIX, getDialectProcessorPrecedence() * 10);
//    }
//
//    public ComponentIncludeProcessor createIncludeProcessor() {
//        return new ComponentIncludeProcessor(PREFIX, getDialectProcessorPrecedence() * 10 + 1);
//    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        HashSet<IProcessor> processors = new HashSet<>();

        return processors;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Collections.singleton("items");
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                Items items = applicationContext.getBean(Items.class);
                items.setContext(context);

                return items;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return false;
            }
        };
    }

    public AnecdotDialect() {
        super("A dialect", PREFIX, 900);
    }
}
