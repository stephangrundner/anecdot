package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.ContentService;
import info.anecdot.content.Item;
import info.anecdot.web.RequestLocaleResolver;
import info.anecdot.web.*;
import org.apache.catalina.connector.Connector;
import org.apache.commons.io.IOUtils;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ThymeleafAutoConfiguration.class})
@PropertySources({
        @PropertySource("default.properties"),
        @PropertySource(ignoreResourceNotFound = true, value = {
                "file:./anecdot.properties",
                "file:/etc/anecdot/anecdot.properties"})})
@Import({Starter.Web.class, Starter.WebSecurity.class})
@EnableCaching
public class Starter implements ApplicationRunner {

    protected class Web implements WebMvcConfigurer {

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
        protected RequestFilter requestFilter() {
            return new RequestFilter();
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
                    .setCacheControl(CacheControl.empty())
                    .resourceChain(false)
//                    .resourceChain(true)
                    .addResolver(resourceResolverDispatcher);
        }

        @Override
        public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
            resolvers.add((request, response, handler, e) -> {
                Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
                if (status == null) {

                    if (e instanceof FileNotFoundException) {
                        status = HttpStatus.NOT_FOUND.value();
                    } else {
                        status = HttpStatus.INTERNAL_SERVER_ERROR.value();
                    }
                }

                RequestLocaleResolver localeResolver = (RequestLocaleResolver) localeResolver();
                ModelAndView modelAndView = new ModelAndView();
                modelAndView.setViewName("error");
                modelAndView.addObject("status", status);
                modelAndView.addObject("message", e.getMessage());
                modelAndView.addObject("e", e);
//                modelAndView.addObject("locale", localeResolver.resolveLocale(request));

                e.printStackTrace();

                return modelAndView;
            });
        }

        @Bean
        protected ShallowEtagHeaderFilter etagHeaderFilter() {
            ShallowEtagHeaderFilter etagHeaderFilter = new ShallowEtagHeaderFilter() {
                @Override
                protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                    Item item = (Item) request.getAttribute(Item.class.getName());
                    if (item != null) {
                        try (InputStream is = IOUtils.toInputStream(item.getId().toString(), "UTF-8")) {
                            return super.generateETagHeaderValue(is, isWeak);
                        }
                    }
                    return null;
                }
            };

            return etagHeaderFilter;
        }

        @Bean
        protected LocaleResolver localeResolver() {
            return new RequestLocaleResolver();
        }

        @Bean
        protected FunctionsExtension functionsExtension(ApplicationContext applicationContext) {
            return new FunctionsExtension(applicationContext);
        }

        @Bean(name = "pebbleLoader")
        protected Loader<?> fileLoaderDecorator(ContentService contentService) {
            return new FileLoaderDecorator(contentService, new FileLoader());
        }

    }

    @EnableWebSecurity
    @ComponentScan(
            basePackageClasses = KeycloakSecurityComponents.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "org.keycloak.adapters.springsecurity.management.HttpSessionManager"))
    @ConditionalOnProperty(value = "anecdot.security", havingValue = "enabled")
    protected class WebSecurity extends KeycloakWebSecurityConfigurerAdapter {

        @Bean
        public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
            return new KeycloakSpringBootConfigResolver();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
            keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
            auth.authenticationProvider(keycloakAuthenticationProvider);
        }

        @Override
        protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
            return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            super.configure(http);
            http.authorizeRequests()
                    .antMatchers("/theme/**").permitAll()
                    .anyRequest().access("@securityService.hasAccess()");
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Environment environment = applicationContext.getEnvironment();
        ContentService contentService = applicationContext.getBean(ContentService.class);
        contentService.reloadSites(environment);

        File configFile = File.createTempFile("thumbor", ".conf");
        configFile.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(configFile);
             PrintStream printer = new PrintStream(outputStream)) {

            printer.println("LOADER='thumbor.loaders.file_loader'");
            printer.println("FILE_LOADER_ROOT_PATH='/'");
            printer.printf("STORAGE='%s'\n", "thumbor.storages.file_storage");
//            printer.printf("STORAGE_EXPIRATION_SECONDS=%d\n", 60 * 60);
//            printer.printf("FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE='%s'\n", "thumbor.result_storages.file_storage");
//            printer.printf("RESULT_STORAGE_FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE_STORES_UNSAFE=%s\n", "True");;
        }

        Integer port = environment.getProperty("thumbor.port", Integer.class);
        String loggingLevel = environment.getProperty("thumbor.debug", Boolean.class, false) ? "DEBUG" : "INFO";
        ProcessBuilder builder = new ProcessBuilder()
                .command("thumbor",
                        "-p", Integer.toString(port),
                        "-l", loggingLevel,
                        "-c", configFile.toString());

        File directory = Paths.get(".").toRealPath().toFile();
        builder.directory(directory);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        Logger LOG = LoggerFactory.getLogger("thumbor");
        try (InputStreamReader reader = new InputStreamReader(process.getInputStream());
             BufferedReader buffer = new BufferedReader(reader);
             PrintStream printer = new PrintStream(System.out)) {

            String line;
            while (process.isAlive() && (line = buffer.readLine()) != null) {
                printer.println(line);
                LOG.debug(line);
            }
        }

        int exitValue = process.waitFor();
        LOG.debug("Thumbor exited: " + exitValue);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Starter.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.SERVLET)
                .headless(true)
                .run(args);
    }
}

