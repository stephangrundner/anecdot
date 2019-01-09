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
    private int ordinal;

    private String name;

    @OneToMany(mappedBy = "sequence", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("ordinal ASC")
    private final List<Fragment> children = new ArrayList<>();

    public Fragment getFragment() {
        return fragment;
    }

    void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
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
            fragment.setOrdinal(children.size() - 1);

            return true;
        }

        return false;
    }
}
