package info.anecdot.security;

import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class Permission {

    public enum Kind {
        ALLOW,
        DENY
    }

    private final Kind kind;
    private final String pattern;
    private final List<String> authorities;
    private List<String> users;

    public Kind getKind() {
        return kind;
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getAuthorities() {
        return Collections.unmodifiableList(authorities);
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public Permission(Kind kind, String pattern, List<String> authorities) {
        this.kind = kind;
        this.pattern = pattern;
        this.authorities = authorities;
    }
}
