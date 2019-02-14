package info.anecdot.content;

import java.nio.file.Path;
import java.util.UUID;

/**
 * @author Stephan Grundner
 */
public class Item extends Payload {

    private final UUID id = UUID.randomUUID();

    private Site site;
    private String uri;

    private String type;

    public UUID getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Path getFile() {
        if (site != null) {
            Path base = site.getBase();
            String relativePathToFile = uri;
            if (relativePathToFile.charAt(0) == '/') {
                relativePathToFile = relativePathToFile.substring(1);
            }

            Path file = base.resolve(relativePathToFile);

            return file;
        }

        return null;
    }
}
