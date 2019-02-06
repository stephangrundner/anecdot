package info.anecdot.security;

import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class Access {

    private final String path;
    private final List<Permission> permissions;

    public String getPath() {
        return path;
    }

    public List<Permission> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    public Access(String path, List<Permission> permissions) {
        this.path = path;
        this.permissions = permissions;
    }
}
