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
//    @Column(unique = true)
    private Path directory;

    @Column(unique = true)
    private String name;

    @ElementCollection
    @CollectionTable(name = "host_name",
            joinColumns = @JoinColumn(name = "host_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "value"}))
    @Column(name = "value")
    private final Set<String> aliases = new LinkedHashSet<>();

    private String templates;
    private String home;

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public String getTemplates() {
        return templates;
    }

    public void setTemplates(String templates) {
        this.templates = templates;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }
}
