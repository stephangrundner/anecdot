package info.anecdot.content;

import org.springframework.beans.factory.annotation.Autowired;
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
public class PageController {

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemService itemService;

    @GetMapping(path = "/page")
    protected ModelAndView byId(@RequestParam(name = "uri") String uri,
                                HttpServletRequest request) {
        Item item = (Item) request.getAttribute(Item.class.getName());
        if (item == null) {
            item = itemService.findItemByRequestAndUri(request, uri);
            if (item == null) {
                throw new RuntimeException("No item found for uri " + uri);
            }
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("page", itemService.toMap(item));

        modelAndView.setViewName(item.getType());

        return modelAndView;
    }
}
