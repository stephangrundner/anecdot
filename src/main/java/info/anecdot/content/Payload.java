package info.anecdot.content;

import org.springframework.data.annotation.Transient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stephan Grundner
 */
public class Payload {

    @Transient
    private Payload parent;
    private Map<String, List<Payload>> sequences;

    private Map<String, String> attributes;
    private String text;

    public Payload getParent() {
        return parent;
    }

    public void setParent(Payload parent) {
        this.parent = parent;
    }

    public Map<String, List<Payload>> getSequences() {
        if (sequences == null) {
            sequences = new LinkedHashMap<>();
        }

        return sequences;
    }

    public void setSequences(Map<String, List<Payload>> sequences) {
        this.sequences = sequences;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }

        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
