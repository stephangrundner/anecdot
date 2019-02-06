package info.anecdot.content;

import org.springframework.data.annotation.Transient;

import java.util.*;

/**
 * @author Stephan Grundner
 */
public abstract class Payload {

    public static class Sequence {

        @Transient
        private Fragment owner;
        private String name;

        private List<Payload> payloads = new ArrayList<>();

        public Fragment getOwner() {
            return owner;
        }

        public void setOwner(Fragment owner) {
            this.owner = owner;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Payload> getPayloads() {
            return Collections.unmodifiableList(payloads);
        }

        public void setPayloads(List<Payload> payloads) {
            this.payloads = payloads;
        }

        public void addPayload(Payload payload) {
            if (payloads.add(payload)) {
                payload.setOwner(this);
            }
        }
    }

    @Transient
    private Sequence owner;

    private Map<String, String> attributes = new LinkedHashMap<>();

    public Sequence getOwner() {
        return owner;
    }

    public void setOwner(Sequence owner) {
        this.owner = owner;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
