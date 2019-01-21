package info.anecdot.config;

import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
public final class PropertyResolverUtils {

//    private static <T> List<T> getProperties(PropertyResolver propertyResolver, ConversionService conversionService, String key, Class<T> targetElementType) {
//        class StringArrayList extends ArrayList<T> {
//            public StringArrayList() { }
//        }
//
//        return propertyResolver.getProperty(key, StringArrayList.class).stream()
//                .map(it -> conversionService.convert(it, targetElementType))
//                .collect(Collectors.toList());
//    }

//    public static <T> List<T> getProperties(ApplicationContext applicationContext, String key, Class<T> targetElementType) {
//        Environment environment = applicationContext.getEnvironment();
//        ConversionService conversionService = applicationContext.getBean(ConversionService.class);
//        return getProperties(environment, conversionService, key, targetElementType);
//    }

    private static List<String> getProperties(Environment environment, String key, List<String> defaultValues) {
        class StringArrayList extends ArrayList<String> {
            private StringArrayList(Collection<? extends String> c) {
                super(c);
            }

            public StringArrayList() { }
        }

        return environment.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
    }

    public static List<String> getProperties(Environment environment, String key) {
        return getProperties(environment, key, Collections.emptyList());
    }

    private PropertyResolverUtils() {}
}
