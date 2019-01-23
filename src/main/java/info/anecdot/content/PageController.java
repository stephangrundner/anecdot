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
    protected ModelAndView byId(@RequestParam(name = "id") Long id,
                                HttpServletRequest request) {
        Item page = itemService.findPageById(id);
        if (page == null) {
            throw new RuntimeException("No page found for id " + id);
        }

        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.addAllObjects(pageService.toMap(page));
        modelAndView.addObject("page", itemService.toMap(page));

        String hostName = siteService.resolveHostName(request);
        modelAndView.addObject("#host", hostName);

        modelAndView.setViewName(page.getType());

        return modelAndView;
    }
}
