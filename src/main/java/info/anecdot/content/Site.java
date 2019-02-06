package info.anecdot.content;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Document
public class Site {

//    @Id
//    private String id;

    @Id
//    @Indexed(unique = true)
    private String host;

    private Set<String> aliases = new LinkedHashSet<>();

    private Path base;
    private String theme;

    private String home;

//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public Path getBase() {
        return base;
    }

    public void setBase(Path base) {
        this.base = base;
//        this.base = base.toString();
    }

    public Path getTheme() {
        return Paths.get(theme);
    }

    public void setTheme(Path theme) {
//        this.theme = theme;
        this.theme = theme.toString();
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }
}
