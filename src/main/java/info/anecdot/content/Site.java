package info.anecdot.content;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Stephan Grundner
 */
public class Site {

    private final Set<String> hosts = new LinkedHashSet<>();
    private final Map<String, Item> pages = new LinkedHashMap<>();

    private Path base;
    private Path theme;

    private String home;

    public Set<String> getHosts() {
        return hosts;
    }

    public Collection<Item> getPages() {
        return Collections.unmodifiableCollection(pages.values());
    }

//    public Set<String> getFiles() {
//        return Collections.unmodifiableSet(pages.keySet());
//    }

    public Item getPage(Path file) {
        return pages.get(file);
    }

    public Item putPage(Item item) {
        String path = item.getUri();
        Item replaced = pages.put(path, item);
        if (replaced != null) {
            replaced.setSite(null);
        }

        item.setSite(this);

        return replaced;
    }

    public Path getBase() {
        return base;
    }

    public void setBase(Path base) {
        this.base = base;
    }

    public Path getTheme() {
        return theme;
    }

    public void setTheme(Path theme) {
        this.theme = theme;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }
}
