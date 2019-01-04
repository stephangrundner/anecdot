package info.anecdot.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Stephan Grundner
 */
@Entity
@DiscriminatorValue("fragment")
public class Fragment extends Payload {

    @OneToMany(mappedBy = "fragment", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @MapKey(name = "name")
    private Map<String, Sequence> sequences = new LinkedHashMap<>();

    public Map<String, Sequence> getSequences() {
        return Collections.unmodifiableMap(sequences);
    }

    public boolean appendPayload(String name, Payload payload) {
        Sequence sequence = sequences.get(name);
        if (sequence == null) {
            sequence = new Sequence();
            sequence.setName(name);
            sequence.setFragment(this);
            sequences.put(name, sequence);
        }

        return sequence.appendPayload(payload);
    }
}
