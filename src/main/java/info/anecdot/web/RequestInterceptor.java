package info.anecdot.web;

import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.servlet.*;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author Stephan Grundner
 */
public class RequestInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptor.class);

    @Autowired(required = false)
    private LocaleResolver localeResolver;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ViewResolver viewResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Locale locale = localeResolver != null ? localeResolver.resolveLocale(request) : Locale.ROOT;
        String requestUri = new UrlPathHelper().getRequestUri(request);

        ErrorProperties errorProperties = serverProperties.getError();
        if (requestUri.equals(errorProperties.getPath())) {

            return true;
        }

        if (!requestUri.startsWith(ResourceResolverDispatcher.THEME_URL_PATH_PREFIX)) {
            Item item = itemService.findItemByRequestAndUri(request, request.getRequestURI());

            if (item != null) {
                ModelAndView modelAndView = new ModelAndView();
                modelAndView.addObject("page", itemService.toMap(item));
                Map<String, Object> params = new LinkedHashMap<>();
                Enumeration<String> parameterNames = request.getParameterNames();
                while (parameterNames.hasMoreElements()) {
                    String name = parameterNames.nextElement();
                    String[] values = request.getParameterValues(name);
                    params.put(name, Arrays.asList(values));
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        params.put(name + i, value);
                    }
                }

                modelAndView.addObject("params", params);
                modelAndView.setViewName(item.getType());
                View view = viewResolver.resolveViewName(modelAndView.getViewName(), locale);
                view.render(modelAndView.getModel(), request, response);

                return false;
            }
        }

        return true;
    }
}
