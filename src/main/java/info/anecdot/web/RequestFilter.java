package info.anecdot.web;

import info.anecdot.content.ContentService;
import info.anecdot.content.Item;
import info.anecdot.content.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Stephan Grundner
 */
public class RequestFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestFilter.class);

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private ContentService contentService;

    @Autowired
    private ViewResolver viewResolver;

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private ItemHandler itemHandler;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = new UrlPathHelper().getRequestUri(request);

        ErrorProperties errorProperties = serverProperties.getError();
        if (!requestUri.equals(errorProperties.getPath()) && !requestUri.startsWith("/theme")) {
            String host = request.getServerName();
            Site site = contentService.findSiteByHost(host);
            request.setAttribute(Site.class.getName(), site);

            if (site.isBusy()) {
                ModelAndView modelAndView = new ModelAndView();
                modelAndView.setViewName("busy");
                try {
                    Locale locale = localeResolver.resolveLocale(request);
                    View view = viewResolver.resolveViewName(modelAndView.getViewName(), locale);
                    view.render(modelAndView.getModel(), request, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return;
            }

            Item item = contentService.findItemBySiteAndUri(site, request.getRequestURI());
            if (item != null) {
                request.setAttribute(Item.class.getName(), item);
                itemHandler.handleRequest(request, response);

                return;
            }
        }

        chain.doFilter(request, response);
    }
}
