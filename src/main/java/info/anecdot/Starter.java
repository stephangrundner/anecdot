package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.FileObserver;
import info.anecdot.content.PathReadingConverter;
import info.anecdot.content.PathWritingConverter;
import info.anecdot.content.SiteService;
import info.anecdot.servlet.PebbleLoaderDecorator;
import info.anecdot.servlet.RequestInterceptor;
import info.anecdot.servlet.ResourceResolverDispatcher;
import info.anecdot.servlet.ThumborRunner;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@PropertySources({
        @PropertySource("default.properties"),
        @PropertySource(ignoreResourceNotFound = true, value = {
                "file:./anecdot.properties",
                "file:/etc/anecdot/anecdot.properties"})})
public class Starter implements ApplicationRunner, WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);

    private static boolean init = false; {
        if (init) {
            throw new IllegalStateException();
        }
        init = true;
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SiteService siteService;

    @Bean
    @Scope("prototype")
    @ConfigurationProperties(prefix = "server.ajp", ignoreInvalidFields = true)
    protected Connector ajpConnector() {
        return new Connector("AJP/1.3");
    }

    @Bean
    @ConditionalOnProperty(name = "server.ajp.port")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webServerFactoryCustomizer(Connector ajpConnector) {
        return factory -> {
            factory.addAdditionalTomcatConnectors(ajpConnector);
        };
    }

    @Bean
    protected RequestInterceptor requestInterceptor() {
        return new RequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    protected ThumborRunner thumborRunner() {
        return new ThumborRunner();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        RequestInterceptor requestInterceptor =
                applicationContext.getBean(RequestInterceptor.class);
        registry.addInterceptor(requestInterceptor).order(0);
    }

    @Bean
    protected ResourceResolverDispatcher resourceResolverDispatcher() {
        return new ResourceResolverDispatcher();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ResourceResolverDispatcher resourceResolverDispatcher =
                applicationContext.getBean(ResourceResolverDispatcher.class);

        registry.addResourceHandler("/**")
                .resourceChain(false)
                .addResolver(resourceResolverDispatcher);
    }

    @Bean
    public Loader<?> pebbleLoader() {
        return new PebbleLoaderDecorator(new FileLoader());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Executors.newSingleThreadExecutor().submit(() -> {
            FileObserver propertiesObserver = new FileObserver(watchService) {
                @Override
                protected void modified(Path file) throws Exception  {
                    siteService.reloadProperties(file);
                }

                @Override
                protected void created(Path file) throws Exception {
                    modified(file);
                }
            };
            Environment environment = applicationContext.getEnvironment();
            String filePath = environment.getProperty("anecdot.sites.properties");
            Path file = Paths.get(filePath);
            try {
                siteService.reloadProperties(file);
            } catch (Exception e) {
                LOG.error("Error while loading properties file " + file, e);
            }
            propertiesObserver.observe(file);
        });
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new PathReadingConverter());
        converters.add(new PathWritingConverter());
        MongoCustomConversions customConversions = new MongoCustomConversions(converters);

        return customConversions;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Starter.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.SERVLET)
                .headless(true)
                .run(args);
    }
}

