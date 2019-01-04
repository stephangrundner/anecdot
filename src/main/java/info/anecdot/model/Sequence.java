package info.anecdot.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"fragment_id", "name"}))
public class Sequence extends Identifiable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "fragment_id")
    private Fragment fragment;

    private String name;

    @OneToMany(mappedBy = "sequence", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderColumn(name = "ordinal")
    private final List<Payload> payloads = new ArrayList<>();

    public Fragment getFragment() {
        return fragment;
    }

    void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public List<Payload> getPayloads() {
        return Collections.unmodifiableList(payloads);
    }

    public boolean appendPayload(Payload payload) {
        if (payloads.add(payload)) {
            payload.setSequence(this);

            return true;
        }

        return false;
    }

    public Sequence() { }
}
