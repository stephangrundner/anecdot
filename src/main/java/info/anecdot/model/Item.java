package info.anecdot.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import info.anecdot.rest.ItemSerializer;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Stephan Grundner
 */
@Entity
@SecondaryTable(name = "item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "uri"}))
@DiscriminatorValue("item")
@JsonSerialize(using = ItemSerializer.class)
public class Item extends Fragment {

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id", table = "item")
    private Host host;

    @Column(table = "item")
    private String uri;

    @Column(table = "item")
    private String type;

    @Column(table = "item")
    private LocalDateTime created;

    @Column(table = "item")
    private LocalDateTime modified;

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
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

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }
}
