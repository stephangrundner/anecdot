package info.anecdot.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

        ModelAndView modelAndView = new ModelAndView();

        Item item = (Item) request.getAttribute(Item.class.getName());
        if (item == null) {
            item = itemService.findItemByRequestAndUri(request, uri);
            if (item == null) {
                throw new RuntimeException("No item found for uri " + uri);
            }
        }
        modelAndView.addObject("page", itemService.toMap(item));

        Map<String, Object> params = new LinkedHashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = request.getParameterValues(name);
            params.put(name, Arrays.asList(values));
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                params.put(name + i, value);
            }
        }

        modelAndView.addObject("params", params);

        modelAndView.setViewName(item.getType());

        return modelAndView;
    }
}
