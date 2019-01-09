package info.anecdot.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import info.anecdot.rest.ItemSerializer;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Stephan Grundner
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "uri"}))
//@SecondaryTable(name = "document",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "uri"}))
//@DiscriminatorValue("document")
@JsonSerialize(using = ItemSerializer.class)
public class Document extends Fragment {

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id")
    private Host host;

    private String uri;
    private String type;
    private LocalDateTime created;
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
