package info.anecdot.servlet;

import info.anecdot.model.Item;
import info.anecdot.model.ItemService;
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
    private ItemService itemService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Item item = itemService.findItemByRequest(request);

        if (item != null) {
            RequestDispatcher dispatcher = request.getServletContext()
                    .getRequestDispatcher("/item?id=" + item.getId());
            dispatcher.forward(request, response);

            return false;
        }

        return true;
    }
}
