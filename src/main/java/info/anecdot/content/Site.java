package info.anecdot.content;

import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
public class Site {

    private String host;

    private Path base;
    private Path theme;
    private String home;

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
}
