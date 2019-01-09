package info.anecdot.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
//@DiscriminatorValue("fragment")
public class Fragment extends Identifiable {

    @OneToMany(mappedBy = "fragment", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @MapKey(name = "name")
    private Map<String, Sequence> sequences = new LinkedHashMap<>();

    @Lob
    @Column(name = "[text]")
    private String text;

    @ManyToOne
    private Sequence sequence;

    @Access(AccessType.PROPERTY)
    private String propertyPath;

    public Map<String, Sequence> getSequences() {
        return Collections.unmodifiableMap(sequences);
    }

    public boolean appendChild(String name, Fragment payload) {
        Sequence sequence = sequences.get(name);
        if (sequence == null) {
            sequence = new Sequence();
            sequence.setName(name);
            sequence.setFragment(this);
            sequences.put(name, sequence);
        }

        return sequence.appendChild(payload);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Sequence getSequence() {
        return sequence;
    }

    void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public int getOrdinal() {
        return sequence.getChildren().indexOf(this);
    }

    public Fragment getParent() {
        if (sequence != null) {
            return sequence.getFragment();
        }

        return null;
    }

    public String getPropertyPath() {
        LinkedList<String> segments = new LinkedList<>();

        Fragment payload = this;
        do {
            if (payload instanceof Document)
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
