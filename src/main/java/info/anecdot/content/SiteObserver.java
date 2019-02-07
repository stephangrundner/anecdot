package info.anecdot.content;

import info.anecdot.io.AbstractDirectoryObserver;
import info.anecdot.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;

/**
 * @author Stephan Grundner
 */
@Component
@Scope("prototype")
public class SiteObserver extends AbstractDirectoryObserver {

    private static final Logger LOG = LoggerFactory.getLogger(SiteObserver.class);

    private final Site site;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SecurityService securityService;

    public Site getSite() {
        return site;
    }

    @Override
    protected Path getRoot() {
        return site.getBase();
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

        Item item = itemService.loadItem(site, file, true);

        "".toString();
    }

    @Override
    protected void visited(Path file) {
        try {
            reload(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void created(Path file) throws Exception {
        reload(file);
    }

    @Override
    protected void modified(Path file) throws Exception {
        reload(file);
    }

    @Override
    protected void deleted(Path path, boolean file) throws Exception { }

    @Override
    protected void overflow(WatchKey key, WatchEvent<?> event) throws Exception {
        throw new Error();
    }

    @Override
    protected void error(WatchKey key, WatchEvent<?> event, Exception e) {
        throw new Error();
    }

    public SiteObserver(Site site) {
        this.site = site;
    }
}
