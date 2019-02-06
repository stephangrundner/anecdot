package info.anecdot.content;

import info.anecdot.security.SecurityService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Stephan Grundner
 */
@Service
public class ObservationService {

    private static final Logger LOG = LoggerFactory.getLogger(ObservationService.class);

    private final Map<Site, DirectoryObserver> observers = new IdentityHashMap<>();

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemLoader itemLoader;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityService securityService;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private boolean accept(Path path) {
        String fileName = path.getFileName().toString();
        String extension = FilenameUtils.getExtension(fileName);
        return !fileName.startsWith(".") && "xml".equals(extension);
//            TODO Also check if file is not ignored by .pageignore file!
    }

    protected void reload(Site site, Path file) throws Exception {
        if (".access".equals(file.getFileName().toString())) {
            securityService.reloadRestriction(site, file);
            return;
        }

        if (!accept(file)) {
            return;
        }

        Item item = itemLoader.loadItem(site.getBase(), file);
        item.setSite(site);
        item = itemService.saveItem(site, item);

        Cache cache = cacheManager.getCache(ItemService.ITEM_BY_URI_CACHE);
        cache.evict(item.getUri());
        if (item.getUri().equals(site.getHome())) {
            cache.evict("/");
        }

        LOG.info("Reloaded file {}", file);
    }

    public void start(Site site) throws IOException {
        if (observers.containsKey(site)) {
            throw new IllegalStateException("Observer already running for " + site.getBase());
        }

        DirectoryObserver observer = new DirectoryObserver() {
            @Override
            protected void visited(Path file) {
                try {
                    reload(site, file);
                } catch (Exception e) {
                    LOG.error("Error while reloading file " + file, e);
                }
            }

            @Override
            protected void created(Path file) {
                visited(file);
            }

            @Override
            protected void modified(Path file) {
                visited(file);
            }

            @Override
            protected void deleted(Path path, boolean file) {
                if (!accept(path)) {
                    return;
                }

                LOG.info("Deleted {}", path);
            }
        };

        observers.put(site, observer);
        executorService.submit(() -> observer.start(site.getBase()));
    }

    public void stop(Site site) throws IOException {
        DirectoryObserver observer = observers.remove(site);
        if (observer == null) {
            throw new IllegalArgumentException("No observer found for " + site.getBase());
        }
        observer.stop();
    }

    public void stopAll() throws IOException {
        for (DirectoryObserver observer : observers.values()) {
            observer.stop();
        }
        observers.clear();
    }
}
