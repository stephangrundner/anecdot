package info.anecdot;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.config.PropertyResolverUtils;
import info.anecdot.content.Site;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableJpaRepositories
@PropertySource("anecdot.properties")
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
		registry.addResourceHandler("/**").addResourceLocations("file:" + themeDirectory);

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
		Environment environment = applicationContext.getEnvironment();
		List<String> keys = PropertyResolverUtils.getProperties(environment, "anecdot.sites");
		for (String key : keys) {
			Site site = new Site();
			String prefix = String.format("anecdot.site.%s", key);

			List<String> names = PropertyResolverUtils.getProperties(environment, prefix + ".hosts");
			site.getHosts().addAll(names);

			String content = environment.getProperty(prefix + ".content");
			site.setContent(Paths.get(content));

			String theme = environment.getProperty(prefix + ".theme");
			site.setTheme(Paths.get(theme));

			site.setHome(environment.getProperty(prefix + ".home", "/home"));

			siteService.reloadProperties(site);
			siteService.saveSite(site);
		}

		ExecutorService executorService = Executors.newCachedThreadPool();

		for (Site site : siteService.findAllSites()) {
			executorService.submit(() -> {
				try {
					siteService.observe(site);
				} catch (Exception e) {
					LOG.error("Error while observing " + site.getContent(), e);
				}
			});
		}
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(Starter.class)
//				.initializers(new PropertiesFileInitializer())
				.bannerMode(Banner.Mode.OFF)
				.web(WebApplicationType.SERVLET)
				.headless(true)
				.run(args);
	}
}

