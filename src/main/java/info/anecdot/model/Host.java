package info.anecdot.model;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Entity
public class Host extends Identifiable {

    @Convert(converter = PathConverter.class)
    @Column(unique = true)
    private Path directory;

    @Deprecated
    @Column
    private String name;

    @ElementCollection
    @CollectionTable(name = "host_name",
            joinColumns = @JoinColumn(name = "host_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "value"}))
    @Column(name = "value")
    private final Set<String> names = new LinkedHashSet<>();

    private String home;

    @ElementCollection
    @CollectionTable(name = "hidden_path",
            joinColumns = @JoinColumn(name = "host_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "pattern"}))
    @Column(name = "pattern")
    private final Set<String> hidden = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "configuration",
            joinColumns = @JoinColumn(name = "host_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "prop"}))
    @Column(name = "value")
    @MapKeyColumn(name = "prop")
    private final Map<String, String> properties = new HashMap<>();

    private LocalDateTime lastModified;

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getNames() {
        return names;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public Set<String> getHidden() {
        return hidden;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
