package info.anecdot;

import info.anecdot.config.PropertiesFileInitializer;
import info.anecdot.model.Host;
import info.anecdot.model.HostService;
import info.anecdot.servlet.RequestInterceptor;
import info.anecdot.servlet.ResourceResolverDispatcher;
import info.anecdot.config.PropertyResolverUtils;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.expression.ExpressionObjects;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.context.SpringContextUtils;
import org.thymeleaf.spring5.dialect.SpringStandardDialect;
import org.thymeleaf.spring5.expression.SpringStandardExpressionObjectFactory;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;
import org.thymeleaf.standard.expression.StandardExpressionObjectFactory;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableJpaRepositories
@PropertySource("anecdot.properties")
public class Application implements ApplicationRunner, WebMvcConfigurer {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	private static boolean init = false; {
		if (init) {
			throw new IllegalStateException();
		}
		init = true;
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private SpringTemplateEngine templateEngine;

	@Autowired
	private HostService hostService;

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

	@PostConstruct
	private void customizeTemplateEngine() {
//		SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		ITemplateResolver templateResolver = new ITemplateResolver() {
			@Override
			public String getName() {
				return null;
			}

			@Override
			public Integer getOrder() {
				return null;
			}

			@Override
			public TemplateResolution resolveTemplate(IEngineConfiguration configuration, String ownerTemplate, String template, Map<String, Object> templateResolutionAttributes) {
				IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(configuration);
				Set<String> x = configuration.getExpressionObjectFactory().getAllExpressionObjectNames();
//				ExpressionObjects.
//				expressionParser.parseExpression(StandardExpressionExecutionContext.NORMAL.withTypeConversion(), "");
//				SpringContextUtils.getRequestContext(configuration);
				return null;
			}
		};
//		templateResolver.setApplicationContext(applicationContext);
//		templateResolver.setCheckExistence(true);
//		templateResolver.setSuffix(".html");
//		templateResolver.setTemplateMode(TemplateMode.HTML);
//		templateResolver.setCacheable(false);

		templateEngine.addTemplateResolver(templateResolver);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Environment environment = applicationContext.getEnvironment();
		List<String> keys = PropertyResolverUtils.getProperties(environment, "anecdot.hosts");
		for (String key : keys) {
			Host host = new Host();
			String prefix = String.format("anecdot.host.%s", key);

			List<String> names = PropertyResolverUtils.getProperties(environment, prefix + ".names");
			host.getNames().addAll(names);

			String directory = environment.getProperty(prefix + ".directory");
			host.setDirectory(Paths.get(directory));

			host.setHome(environment.getProperty(prefix + ".home", "/index"));

//			List<String> hidden = PropertyResolverUtils.getProperties(environment, prefix + ".hidden");
//			host.getHidden().addAll(hidden);

			hostService.reloadProperties(host);

			hostService.saveHost(host);
		}

		ExecutorService executorService = Executors.newCachedThreadPool();

		for (Host host : hostService.findAllHosts()) {
			executorService.submit(() -> {
				try {
					hostService.observe(host);
				} catch (Exception e) {
					LOG.error("Error while observing " + host.getDirectory().resolve("content"), e);
				}
			});
		}
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class)
//				.initializers(new PropertiesFileInitializer())
				.bannerMode(Banner.Mode.OFF)
				.run(args);
	}
}

