package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.SiteService;
import info.anecdot.web.*;
import org.apache.catalina.connector.Connector;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.*;

import javax.servlet.RequestDispatcher;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class})
@PropertySources({
        @PropertySource("default.properties"),
        @PropertySource(ignoreResourceNotFound = true, value = {
                "file:./anecdot.properties",
                "file:/etc/anecdot/anecdot.properties"})})
@Import({Starter.Web.class, Starter.WebSecurity.class})
@EnableCaching
@EnableScheduling
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
        protected RequestInterceptor requestInterceptor() {
            return new RequestInterceptor();
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

                ModelAndView modelAndView = new ModelAndView();
                modelAndView.setViewName("error");
                modelAndView.addObject("status", status);
                modelAndView.addObject("message", e.getMessage());
                modelAndView.addObject("e", e);

                e.printStackTrace();

                return modelAndView;
            });
        }

        @Bean
        protected FunctionsExtension pebbleExtension(ApplicationContext applicationContext) {
            return new FunctionsExtension(applicationContext);
        }

        @Bean
        protected Loader<?> pebbleLoader(SiteService siteService) {
            return new LoaderDecorator(siteService, new FileLoader());
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

//    @Bean
//    public MongoCustomConversions customConversions() {
//        List<Converter<?, ?>> converters = new ArrayList<>();
//        converters.add(new PathReadingConverter());
//        converters.add(new PathWritingConverter());
//
//        return new MongoCustomConversions(converters);
//    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Environment environment = applicationContext.getEnvironment();
        SiteService siteService = applicationContext.getBean(SiteService.class);
        siteService.reloadSitesSettings(environment);


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
                        "-l", "DEBUG",
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

