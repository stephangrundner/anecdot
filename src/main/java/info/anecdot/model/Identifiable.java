package info.anecdot.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Stephan Grundner
 */
@MappedSuperclass
public abstract class Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

//    public void setId(Long id) {
//        this.id = id;
//    }
}
