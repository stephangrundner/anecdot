package info.anecdot.servlet;

import info.anecdot.model.Document;
import info.anecdot.model.DocumentService;
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
    private DocumentService documentService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Document document = documentService.findDocumentByRequest(request);

        if (document != null) {
            RequestDispatcher dispatcher = request.getServletContext()
                    .getRequestDispatcher("/document?id=" + document.getId());
            dispatcher.forward(request, response);

            return false;
        }

        return true;
    }
}
