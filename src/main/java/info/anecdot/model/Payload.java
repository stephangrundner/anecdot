package info.anecdot.model;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind")
public abstract class Payload extends Identifiable {

    @ManyToOne
    private Sequence sequence;

    @Column(updatable = false, insertable = false)
    private String kind;

    @Access(AccessType.PROPERTY)
    private String propertyPath;

    public Sequence getSequence() {
        return sequence;
    }

    void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public int getOrdinal() {
        return sequence.getPayloads().indexOf(this);
    }

    public String getKind() {
        return kind;
    }

    public Payload getParent() {
        if (sequence != null) {
            return sequence.getFragment();
        }

        return null;
    }

    public String getPropertyPath() {
        LinkedList<String> segments = new LinkedList<>();

        Payload payload = this;
        do {
            if (payload instanceof Item)
                break;

            segments.addFirst(String.format("%s[%d]",
                    payload.sequence.getName(),
                    payload.getOrdinal()));

            payload = payload.getParent();
        } while (payload != null);

        return segments.stream().collect(Collectors.joining("."));
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }
}
