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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@PropertySources({
        @PropertySource("default.properties"),
        @PropertySource(ignoreResourceNotFound = true, value = {
                "file:./anecdot.properties",
                "file:/etc/anecdot/anecdot.properties"})})
@Import({Starter.Web.class, Starter.Security.class})
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
                    .resourceChain(false)
                    .addResolver(resourceResolverDispatcher);
        }
    }

    @EnableWebSecurity
    @ComponentScan(
            basePackageClasses = KeycloakSecurityComponents.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "org.keycloak.adapters.springsecurity.management.HttpSessionManager"))
    protected class Security extends KeycloakWebSecurityConfigurerAdapter {

        @Bean
        public KeycloakSpringBootConfigResolver KeycloakConfigResolver() {
            return new KeycloakSpringBootConfigResolver();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
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
                    .anyRequest()//.permitAll();
                    .access("@securityService.hasAccess()");
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @ConfigurationProperties(prefix = "thumbor")
    @Bean
    protected ThumborRunner thumborRunner() {
        return new ThumborRunner();
    }

    @Bean
    protected PebbleExtension pebbleExtension(ApplicationContext applicationContext) {
        return new PebbleExtension(applicationContext);
    }

    @Bean
    protected Loader<?> pebbleLoader(SiteService siteService) {
        return new PebbleLoaderDecorator(siteService, new FileLoader());
    }

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
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Starter.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.SERVLET)
                .headless(true)
                .run(args);
    }
}

