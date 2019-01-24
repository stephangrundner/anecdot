package info.anecdot.content;

import java.util.*;

/**
 * @author Stephan Grundner
 */
public class Fragment extends Payload {

    private final Map<String, Sequence> sequences = new LinkedHashMap<>();

    public Set<String> getSequenceNames() {
        return Collections.unmodifiableSet(sequences.keySet());
    }

    public Collection<Sequence> getSequences() {
        return Collections.unmodifiableCollection(sequences.values());
    }

    public Sequence getSequence(String name) {
        return sequences.get(name);
    }

    public Sequence setSequence(String name, Sequence sequence) {
        Sequence replaced = sequences.put(name, sequence);
        if (replaced != null) {
            replaced.setOwner(null);
            replaced.setName(null);
        }

        if (sequence != null) {
            sequence.setOwner(this);
            sequence.setName(name);
        }

        return replaced;
    }
}
