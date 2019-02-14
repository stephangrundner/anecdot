package info.anecdot.content;

import info.anecdot.security.Access;

import java.util.List;
import java.util.Locale;

/**
 * @author Stephan Grundner
 */
public class Settings {

    private final String path;
    private Locale locale;
    private Access access;
    private List<String> ignorePatterns;

    public String getPath() {
        return path;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public List<String> getIgnorePatterns() {
        return ignorePatterns;
    }

    public void setIgnorePatterns(List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public Settings(String path) {
        this.path = path;
    }
}
