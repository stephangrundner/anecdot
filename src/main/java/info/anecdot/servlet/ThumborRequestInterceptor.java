package info.anecdot.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stephan Grundner
 */
public class ThumborRequestInterceptor implements HandlerInterceptor {

    @Autowired
    private Thumbor thumbor;

    private boolean isThumborRequest(HttpServletRequest request) {
        return request.getParameter("size") != null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isThumborRequest(request)) {
            thumbor.process(request, response);

            return false;
        }

        return true;
    }
}
