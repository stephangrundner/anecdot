package info.anecdot.content;

import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

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
    private Locale locale;

    private Observer observer;
    private boolean busy;

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

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Observer getObserver() {
        return observer;
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
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

    public String toUri(Path file) {
        String uri = base.relativize(file).toString();
        uri = FilenameUtils.removeExtension(uri);
        if (!StringUtils.startsWithIgnoreCase(uri, "/")) {
            uri = "/" + uri;
        }

        return uri;
    }
}
