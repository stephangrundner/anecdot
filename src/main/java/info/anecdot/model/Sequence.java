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
    private final List<Fragment> children = new ArrayList<>();

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

    public List<Fragment> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean appendChild(Fragment fragment) {
        if (children.add(fragment)) {
            fragment.setSequence(this);

            return true;
        }

        return false;
    }
}
