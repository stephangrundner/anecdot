package info.anecdot.servlet;

import info.anecdot.model.Page;
import info.anecdot.model.PageService;
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
    private PageService pageService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Page page = pageService.findPageByRequest(request);

        if (page != null) {
            RequestDispatcher dispatcher = request.getServletContext()
                    .getRequestDispatcher("/page?id=" + page.getId());
            dispatcher.forward(request, response);

            return false;
        }

        return true;
    }
}
