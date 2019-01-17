package info.anecdot.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class FragmentService {

    @Autowired
    private HostService hostService;

    private String getTemplateKey(Fragment fragment) {
        LinkedList<String> segments = new LinkedList<>();

        Fragment current = fragment;
        do {
            String x;
            if (current instanceof Document) {
                x = ((Document) current).getType();
            } else {
                x = current.getSequence().getName();
            }

            segments.addFirst(String.format("%s", x));

            current = current.getParent();
        } while (current != null);

        return segments.stream().collect(Collectors.joining("."));
    }

    public String findTemplate(HttpServletRequest request, Fragment fragment) {
        Host host = hostService.findHostByRequest(request);
        String x = getTemplateKey(fragment);
        String template = host.getProperties().get(x + ".template");

        Path y =host.getDirectory().resolve(template);

        return "file:" + y.toString();
    }

//    public String findTemplate(HttpServletRequest request, Fragment fragment) {
//
//        String x = fragment.getPropertyPath();
////        x = x.replaceAll("\\[0-9+\\]", "/");
//        Document root = (Document) fragment.getRoot();
//        x = root.getType() + "/" +  x.replaceAll("\\[[0-9]+\\]\\.?", "/");
//        if (x.endsWith("/")) {
//            x = x.substring(0, x.length() - 1);
//        }
//        x += ".html";
//
//        Host host = hostService.findHostByRequest(request);
////        String templates = host.getDirectory().resolve("templates").toString();
//        String templates = host.getDirectory().toString();
//        if (!templates.endsWith("/")) {
//            templates += "/";
//        }
//
//        String prefix = "file:" + templates;
//
////        if (fragment instanceof Document) {
////            return prefix +((Document) fragment).getType();
////        }
////        return prefix + fragment.getSequence().getName();
//
//        return prefix + x;
//    }
}
