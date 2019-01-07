package info.anecdot.servlet;

import info.anecdot.model.Host;
import info.anecdot.model.HostService;
import info.anecdot.model.Item;
import info.anecdot.model.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Stephan Grundner
 */
@Controller
@RequestMapping
public class ItemController {

    @Autowired
    private HostService hostService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping(path = "/item")
    protected ModelAndView page(@RequestParam(name = "id") Long id) {

//        Item item = itemService.findItemByRequest(request);
        Item item = itemService.findItemById(id);
        if (item == null) {
            throw new RuntimeException("No item found for id " + id);
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
