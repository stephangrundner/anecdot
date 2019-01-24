package info.anecdot.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

/**
 * @author Stephan Grundner
 */
public class FileObserver {

    private final Logger LOG = LoggerFactory.getLogger(FileObserver.class);

    private final WatchService watchService;

    protected void created(Path file) throws Exception { }
    protected void modified(Path file) throws Exception { }
    protected void deleted(Path path) throws Exception { }
    protected void overflow(WatchKey key, WatchEvent<?> event) throws Exception { }
    protected void error(WatchKey key, WatchEvent<?> event, Exception e) throws Exception {
        LOG.error("Error while observing " + event.context(), e);
    }

    public void observe(Path file) {
        Path directory = file.getParent();

        try {
            WatchKey key;

            key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.OVERFLOW);

            LOG.info("Begin observing {}", file);

            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    try {
                        Object context = event.context();
                        WatchEvent.Kind<?> kind = event.kind();

                        if (context instanceof Path) {
                            final Path parent = (Path) key.watchable();
                            final Path path = parent.resolve((Path) context);

                            if (path.equals(file)) {
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    created(file);
                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    modified(path);
                                } else { /* if (kind == StandardWatchEventKinds.ENTRY_DELETE) { */
                                    deleted(path);
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
        } catch (ClosedWatchServiceException e) {
            LOG.info("Stopped observing {}", file);
        } catch (Exception e) {
            LOG.error("Error while observing " + file, e);
            throw new RuntimeException(e);
        }
    }

    public FileObserver(WatchService watchService) {
        this.watchService = watchService;
    }

    public FileObserver(FileSystem fileSystem) throws IOException {
        this(fileSystem.newWatchService());
    }

    public FileObserver() throws IOException {
        this(FileSystems.getDefault());
    }
}
