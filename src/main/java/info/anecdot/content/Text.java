package info.anecdot.content;

/**
 * @author Stephan Grundner
 */
public class Text extends Payload {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
