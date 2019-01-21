package info.anecdot.model;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Stephan Grundner
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Fragment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @ManyToOne
    private Fragment parent;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "first_child",
            joinColumns = @JoinColumn(name = "fragment_id"),
            inverseJoinColumns = @JoinColumn(name = "first_id"))
    @MapKeyColumn(name = "name")
    private final Map<String, Fragment> children = new LinkedHashMap<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Fragment next;

    @Lob
    @Column(name = "[text]")
    private String text;

    @ElementCollection
    @CollectionTable(name = "adfvsdfv")
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Fragment getParent() {
        return parent;
    }

    private void setParent(Fragment parent) {
        this.parent = parent;
    }

    public Map<String, Fragment> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public Fragment getNext() {
        return next;
    }

    private void setNext(Fragment next) {
        this.next = next;
    }

    public boolean isLast() {
        return getNext() == null;
    }

    public Fragment getLast() {
        Fragment fragment = this;
        while (!fragment.isLast()) {
            fragment = fragment.getNext();
        }

        return fragment;
    }

    public void appendChild(String name, Fragment fragment) {
        Fragment first = children.get(name);
        if (first == null) {
            children.put(name, fragment);
        } else {
            Fragment last = first.getLast();
            last.setNext(fragment);
            fragment.setParent(this);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Fragment getRoot() {
        Fragment fragment = this;
        while (true) {
            Fragment parent = fragment.getParent();
            if (parent == null) {
                return fragment;
            }
            fragment = fragment.getParent();
        }
    }

//    public String getPropertyPath() {
//        LinkedList<String> segments = new LinkedList<>();
//
//        Fragment fragment = this;
//        do {
//            if (fragment instanceof Document)
//                break;
//
//            segments.addFirst(String.format("%s[%d]",
//                    fragment.getName(),
//                    fragment.getOrdinal()));
//
//            fragment = fragment.getParent();
//        } while (fragment != null);
//
//        return segments.stream().collect(Collectors.joining("."));
//    }

//    public void setPropertyPath(String propertyPath) {
//        this.propertyPath = propertyPath;
//    }

//    public String getName() {
//        Fragment parent = getParent();
//        if (parent != null) {
//            return parent.getChildren().entrySet().stream()
//                    .filter(it -> it.getValue().equals(this))
//                    .map(Map.Entry::getKey)
//                    .findFirst().orElse(null);
//        }
//
//        return null;
//    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
