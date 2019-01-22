package info.anecdot.content;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Convert(converter = PathConverter.class)
    @Column(unique = true)
    private Path content;

    @Convert(converter = PathConverter.class)
    private Path theme;

    @ElementCollection
    @CollectionTable(name = "host",
            joinColumns = @JoinColumn(name = "site_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "name"}))
    @Column(name = "name")
    private final Set<String> hosts = new LinkedHashSet<>();

    private String home;

    @ElementCollection
    @CollectionTable(name = "ignored",
            joinColumns = @JoinColumn(name = "host_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "pattern"}))
    @Column(name = "pattern")
    private final Set<String> hidden = new LinkedHashSet<>();

    private LocalDateTime lastModified;

    public Path getContent() {
        return content;
    }

    public void setContent(Path content) {
        this.content = content;
    }

    public Path getTheme() {
        return theme;
    }

    public void setTheme(Path theme) {
        this.theme = theme;
    }

    public Set<String> getHosts() {
        return hosts;
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
}
