package info.anecdot.web;

import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stephan Grundner
 */
public class RequestInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptor.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ThumborRunner thumborRunner;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getParameter("size") != null) {
            thumborRunner.process(request, response);

            return false;
        }

        String requestUri = new UrlPathHelper().getRequestUri(request);
        if (!requestUri.startsWith("/theme/")) {
            Item item = itemService.findItemByRequestAndUri(request, request.getRequestURI());

            if (item != null) {
                request.setAttribute(Item.class.getName(), item);
                RequestDispatcher dispatcher = request.getServletContext()
                        .getRequestDispatcher("/item?uri=" + item.getUri());
                dispatcher.forward(request, response);

                return false;
            }
        }

        return true;
    }
}
