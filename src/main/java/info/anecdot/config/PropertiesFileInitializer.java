package info.anecdot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stephan Grundner
 */
@Deprecated
public class PropertiesFileInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesFileInitializer.class);

    private void prependPrefix(Properties properties, String prefix) {
        if (!prefix.endsWith("."))
            prefix += ".";

        Set<Object> keys = new LinkedHashSet<>(properties.keySet());
        for (Object key : keys) {
            Object value = properties.remove(key);
            properties.put(prefix + key, value);
        }
    }

    private void removeDisallowedKeys(Properties properties, Set<String> disallowedKeys) {
        for (String key : disallowedKeys) {
            Object removed = properties.remove(key);
            if (removed != null) {
                LOG.warn("Removed disallowed property [{}={}]", key, removed);
            }
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            List<String> keys = PropertyResolverUtils.getProperties(environment, "anecdot.hosts");
            for (String key : keys) {
                String prefix = String.format("anecdot.host.%s", key);
                Path directory = Paths.get(environment.getProperty(prefix + ".directory"));
                Path propertiesFile = directory.resolve(".properties");
                if (Files.exists(propertiesFile)) {
                    Resource resource = applicationContext.getResource("file:" + propertiesFile.toRealPath());
                    Properties properties = PropertiesLoaderUtils.loadProperties(resource);

                    // foo=bar -> anecdot.host.xyz.foo=bar
                    prependPrefix(properties, prefix);

                    removeDisallowedKeys(properties,
                            Stream.of(prefix + ".directory")
                                    .collect(Collectors.toSet()));

                    environment.getPropertySources().addFirst(new PropertiesPropertySource(key, properties));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
