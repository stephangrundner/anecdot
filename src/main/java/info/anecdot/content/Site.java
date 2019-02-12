package info.anecdot.content;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Stephan Grundner
 */
public class Site {

    private String host;

    private Path base;
    private Path theme;
    private String home;

    private final Map<String, Item> itemByUri = new LinkedHashMap<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public Set<String> getUris() {
        return Collections.unmodifiableSet(itemByUri.keySet());
    }

    public Collection<Item> getItems() {
        return Collections.unmodifiableCollection(itemByUri.values());
    }

    public Item getItem(String uri) {
        return itemByUri.get(uri);
    }

    public synchronized Item addItem(Item item) {
        String uri = item.getUri();
        Item replaced = itemByUri.put(uri, item);
        if (replaced != null) {
            replaced.setSite(null);
        }

        item.setSite(this);

        return replaced;
    }

    public synchronized boolean removeItem(Item item) {
        Item removed = itemByUri.remove(item.getUri());
        if (removed != null) {
            removed.setSite(null);
            return true;
        }

        return false;
    }
}
