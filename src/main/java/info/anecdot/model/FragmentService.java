package info.anecdot.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Stephan Grundner
 */
@Service
public class FragmentService {

    @Autowired
    private HostService hostService;

    public String findTemplate(HttpServletRequest request, Fragment fragment) {
        Host host = hostService.findHostByRequest(request);
        String templates = host.getDirectory().resolve("templates").toString();
        if (!templates.endsWith("/")) {
            templates += "/";
        }

        String prefix = "file:" + templates;

        if (fragment instanceof Document) {
            return prefix +((Document) fragment).getType();
        }
        return prefix + fragment.getSequence().getName();
    }
}
