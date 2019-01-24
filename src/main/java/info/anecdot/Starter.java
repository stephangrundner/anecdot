package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.FileObserver;
import info.anecdot.content.SiteService;
import info.anecdot.servlet.*;
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
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@PropertySource("anecdot.properties")
public class Starter implements ApplicationRunner, WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);

//    private static <T> List<T> getProperties(PropertyResolver propertyResolver, ConversionService conversionService, String key, Class<T> targetElementType) {
//        class StringArrayList extends ArrayList<T> {
//            public StringArrayList() { }
//        }
//
//        return propertyResolver.getProperty(key, StringArrayList.class).stream()
//                .map(it -> conversionService.convert(it, targetElementType))
//                .collect(Collectors.toList());
//    }
//
//    public static <T> List<T> getProperties(ApplicationContext applicationContext, String key, Class<T> targetElementType) {
//        Environment environment = applicationContext.getEnvironment();
//        ConversionService conversionService = applicationContext.getBean(ConversionService.class);
//        return getProperties(environment, conversionService, key, targetElementType);
//    }
//
//    private static List<String> getProperties(Environment environment, String key, List<String> defaultValues) {
//        class StringArrayList extends ArrayList<String> {
//            private StringArrayList(Collection<? extends String> c) {
//                super(c);
//            }
//
//            public StringArrayList() {
//            }
//        }
//
//        return environment.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
//    }
//
//    public static List<String> getProperties(Environment environment, String key) {
//        return getProperties(environment, key, Collections.emptyList());
//    }

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
    protected ThumborRequestInterceptor thumborRequestInterceptor() {
        return new ThumborRequestInterceptor();
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

        ThumborRequestInterceptor thumborRequestInterceptor =
                applicationContext.getBean(ThumborRequestInterceptor.class);
        registry.addInterceptor(thumborRequestInterceptor)
                .order(1);
    }

    @Bean
    protected ResourceResolverDispatcher resourceResolverDispatcher() {
        return new ResourceResolverDispatcher();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ResourceResolverDispatcher resourceResolverDispatcher =
                applicationContext.getBean(ResourceResolverDispatcher.class);

        Environment environment = applicationContext.getEnvironment();
        String themeDirectory = environment.getProperty("anecdot.theme-directory", "/theme");
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + themeDirectory);

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
            Path file = Paths.get("./sites.properties");
            try {
                siteService.reloadProperties(file);
            } catch (Exception e) {
                LOG.error("Error while loading properties file " + file, e);
            }
            propertiesObserver.observe(file);
        });
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Starter.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.SERVLET)
                .headless(true)
                .run(args);
    }
}

