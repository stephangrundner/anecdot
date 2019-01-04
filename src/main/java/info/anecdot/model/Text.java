package info.anecdot.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Stephan Grundner
 */
@Entity
@DiscriminatorValue("value")
public class Text extends Payload {

    @Column(name = "[text]")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
