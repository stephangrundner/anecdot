package info.anecdot.content;

import java.util.*;

/**
 * @author Stephan Grundner
 */
public abstract class Payload {

    public static class Sequence {

        private Fragment owner;
        private String name;

        private final List<Payload> payloads = new ArrayList<>();

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

        public void addPayload(Payload payload) {
            if (payloads.add(payload)) {
                payload.setOwner(this);
            }
        }
    }

    private Sequence owner;

    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Sequence getOwner() {
        return owner;
    }

    public void setOwner(Sequence owner) {
        this.owner = owner;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
