package info.anecdot.content;

import info.anecdot.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Component
@Scope("prototype")
public class SiteObserver implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SiteObserver.class);

    private final Site site;
    private WatchService watchService;
    private final Set<WatchKey> keys = new LinkedHashSet<>();

    @Autowired
    private SiteService siteService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SecurityService securityService;

    public Site getSite() {
        return site;
    }

    private void reload(Path file) throws Exception {
        String fileName = file.getFileName().toString();
        if (".access".equals(fileName)) {
            securityService.reloadRestriction(site, file);
            return;
        }

        if (!fileName.endsWith(".xml")) {
            LOG.info("Ignoring " + file);

            return;
        }

        itemService.loadItem(site, file, true);

        LOG.info("(Re)loaded " + file);
    }

    private void visited(Path file) {
        try {
            reload(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void created(Path file) throws Exception {
        reload(file);
    }

    private void modified(Path file) throws Exception {
        reload(file);
    }

    private void deleted(Path path, boolean file) throws Exception {
        if (file) {
            String uri = siteService.toUri(site, path);
            Item item = itemService.findItemBySiteAndUri(site, uri);
            if (item != null && site.removeItem(item)) {
                LOG.info("Removed " + path);
            }
        }
    }

    private void overflow(WatchKey key, WatchEvent<?> event) throws Exception {
        throw new Error();
    }

    private void error(WatchKey key, WatchEvent<?> event, Exception e) {
        throw new Error();
    }

    private void observe(Path directory) {
        try {
            LOG.info("Begin observing {}", directory);

            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(this::visited);

            Files.list(directory)
                    .filter(Files::isDirectory)
                    .forEach(this::observe);

            WatchKey key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.OVERFLOW);

            keys.add(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        Path root = site.getBase();

        try {
            FileSystem fileSystem = root.getFileSystem();
            watchService = fileSystem.newWatchService();
            observe(root);

            WatchKey key;

            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    try {
                        Object context = event.context();
                        WatchEvent.Kind<?> kind = event.kind();

                        if (context instanceof Path) {
                            final Path parent = (Path) key.watchable();
                            final Path path = parent.resolve((Path) context);

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                if (Files.isDirectory(path)) {
                                    observe(path);
                                } else {
                                    created(path);
                                }
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                if (!Files.isDirectory(path)) {
                                    modified(path);
                                }
                            } else /* if (kind == StandardWatchEventKinds.ENTRY_DELETE) */ {
                                Path watchable = keys.stream()
                                        .filter(it -> it.watchable().equals(path))
                                        .map(WatchKey::watchable)
                                        .map(Path.class::cast)
                                        .findFirst().orElse(null);

                                if (watchable != null) {
                                    boolean removed = keys.removeIf(it -> {
                                        if (((Path) it.watchable()).startsWith(watchable)) {
                                            it.cancel();
                                            LOG.info("Stopped observing {}", it.watchable());

                                            return true;
                                        }

                                        return false;
                                    });

                                    if (removed) {
                                        deleted(path, false);
                                    }

                                } else {
                                    deleted(path, true);
                                }
                            }

                        } else {
                            overflow(key, event);
                        }

                    } catch (Exception e) {
                        error(key, event, e);
                    } finally {
                        key.reset();
                    }
                }
            }

            watchService.close();
        } catch (ClosedWatchServiceException e) {
            LOG.info("Stopped observing {}", root);
        } catch (Exception e) {
            LOG.error("Uncaught exception", e);
            throw new RuntimeException(e);
        } finally {
            keys.clear();
        }
    }

    public SiteObserver(Site site) {
        this.site = site;
    }
}
