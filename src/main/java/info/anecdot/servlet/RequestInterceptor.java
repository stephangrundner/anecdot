package info.anecdot.servlet;

import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stephan Grundner
 */
public class RequestInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptor.class);

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Item item = itemService.findItemByRequestAndUri(request, request.getRequestURI());

        if (item != null) {
            request.setAttribute(Item.class.getName(), item);
            RequestDispatcher dispatcher = request.getServletContext()
                    .getRequestDispatcher("/page?uri=" + item.getUri());
            dispatcher.forward(request, response);

            return false;
        }

        return true;
    }
}
