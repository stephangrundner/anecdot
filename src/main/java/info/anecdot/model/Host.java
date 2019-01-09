package info.anecdot.model;

import javax.persistence.*;
import java.nio.file.Path;
import java.util.LinkedHashSet;
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
}
