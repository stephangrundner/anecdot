package info.anecdot.web;

import info.anecdot.content.ContentService;
import info.anecdot.content.Item;
import info.anecdot.content.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephan Grundner
 */
@Component
public class ItemHandler implements HttpRequestHandler {

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private ContentService contentService;

    @Autowired
    private ViewResolver viewResolver;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUri = new UrlPathHelper().getRequestUri(request);
        Site site = (Site) request.getAttribute(Site.class.getName());

        ModelAndView modelAndView = new ModelAndView();

        Locale locale = localeResolver.resolveLocale(request);
        modelAndView.addObject("locale", locale);



        Item item = contentService.findItemBySiteAndUri(site, request.getRequestURI());
        if (item != null) {
            request.setAttribute(Item.class.getName(), item);
            modelAndView.addObject("page", contentService.toMap(item));

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
            response.addHeader("Cache-Control", "private, max-age=" + TimeUnit.HOURS.toHours(1));

            try {
                View view = viewResolver.resolveViewName(modelAndView.getViewName(), locale);
                view.render(modelAndView.getModel(), request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
