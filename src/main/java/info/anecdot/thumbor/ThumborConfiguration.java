package info.anecdot.thumbor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Stephan Grundner
 */
@Configuration
//@ComponentScan(basePackageClasses = ThumborConfiguration.class)
public class ThumborConfiguration implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

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
        ThumborRequestInterceptor thumborRequestInterceptor =
                applicationContext.getBean(ThumborRequestInterceptor.class);
        registry.addInterceptor(thumborRequestInterceptor)
                .order(ThumborRequestInterceptor.ORDINAL);
    }
}
