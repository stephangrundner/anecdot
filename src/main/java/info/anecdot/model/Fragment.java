package info.anecdot.model;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Fragment extends Identifiable {

    @OneToMany(mappedBy = "fragment", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @MapKey(name = "name")
    @OrderBy("ordinal ASC")
    private Set<Sequence> sequences = new LinkedHashSet<>();

    @Lob
    @Column(name = "[text]")
    private String text;

    @ManyToOne
    private Sequence sequence;
    private int ordinal;

    @Access(AccessType.PROPERTY)
    private String propertyPath;

    @ElementCollection
    @CollectionTable(name = "fragment_attribute")
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Set<Sequence> getSequences() {
        return Collections.unmodifiableSet(sequences);
    }

    public Sequence getSequence(String name) {
        Collection<Sequence> found = sequences.stream()
                .filter(it -> it.getName().equals(name))
                .collect(Collectors.toSet());

        if (!found.isEmpty()) {
            if (found.size() > 1) {
                throw new IllegalStateException();
            }
            return found.iterator().next();
        }

        return null;
    }

    public boolean appendChild(String name, Fragment payload) {
        Sequence sequence = getSequence(name);
        if (sequence == null) {
            sequence = new Sequence();
            sequence.setName(name);
            sequence.setFragment(this);
            sequence.setOrdinal(sequences.size());
            sequences.add(sequence);
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
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
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

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
