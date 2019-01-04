package info.anecdot;

import info.anecdot.model.Host;
import info.anecdot.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Stephan Grundner
 */
@Controller
@RequestMapping(path = "**")
public class ItemController {

    @Autowired
    private HostService hostService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping
    protected ModelAndView page(HttpServletRequest request) {
        Item item = itemService.findItemByRequest(request);
        if (item == null) {
            throw new RuntimeException("Page not found");
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addAllObjects(itemService.toMap(item));
        modelAndView.addObject("$item", item);

        Host host = item.getHost();
        String templates = host.getTemplates();
        if (!templates.endsWith("/")) {
            templates += "/";
        }

        modelAndView.setViewName("file:" + templates + item.getType());

        return modelAndView;
    }
}
