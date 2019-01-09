package info.anecdot.servlet;

import info.anecdot.model.Host;
import info.anecdot.model.HostService;
import info.anecdot.model.Document;
import info.anecdot.model.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Stephan Grundner
 */
@Controller
@RequestMapping
public class DocumentController {

    @Autowired
    private HostService hostService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping(path = "/document")
    protected ModelAndView byId(@RequestParam(name = "id") Long id,
                                HttpServletRequest request) {
        Document document = documentService.findDocumentById(id);
        if (document == null) {
            throw new RuntimeException("No item found for id " + id);
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addAllObjects(documentService.toMap(document));

        String hostName = hostService.resolveHostName(request);
        modelAndView.addObject("$hostName", hostName);

        Host host = document.getHost();
        String templates = host.getDirectory().resolve("templates").toString();
        if (!templates.endsWith("/")) {
            templates += "/";
        }

        modelAndView.setViewName("file:" + templates + document.getType());

        return modelAndView;
    }
}
