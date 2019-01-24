package info.anecdot.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
public class DirectoryObserver {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryObserver.class);

    private final WatchService watchService;

    private final Set<WatchKey> keys = new LinkedHashSet<>();

    protected void visited(Path file) { }

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

    protected void created(Path file) throws Exception { }
    protected void modified(Path file) throws Exception { }
    protected void deleted(Path path, boolean file) throws Exception { }
    protected void overflow(WatchKey key, WatchEvent<?> event) throws Exception { }
    protected void error(WatchKey key, WatchEvent<?> event, Exception e) { }

    public void stop() throws IOException {
        watchService.close();
        keys.clear();
    }

    public void start(Path directory) {
        observe(directory);

        try {
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
                        try {
                            error(key, event, e);
                        } catch (Exception uncaught) {
                            LOG.error("Uncaught exception", uncaught);
                        }
                    } finally {
                        key.reset();
                    }
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ClosedWatchServiceException e) {
            LOG.info("Stopped observing {}", directory);
        }
    }

    public DirectoryObserver(WatchService watchService) {
        this.watchService = watchService;
    }

    public DirectoryObserver(FileSystem fileSystem) throws IOException {
        this(fileSystem.newWatchService());
    }

    public DirectoryObserver() throws IOException {
        this(FileSystems.getDefault());
    }
}
