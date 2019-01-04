package info.anecdot;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaRepositories
@PropertySource("anecdot.properties")
public class Application {

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

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private SpringTemplateEngine templateEngine;

	@PostConstruct
	private void customizeTemplateEngine() {
		SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		templateResolver.setApplicationContext(applicationContext);
//		templateResolver.setCheckExistence(true);
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCacheable(false);

		templateEngine.addTemplateResolver(templateResolver);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}

