package info.anecdot.rest;

import info.anecdot.model.Fragment;
import info.anecdot.model.Document;

import java.io.IOException;

/**
 * @author Stephan Grundner
 */
public class ItemSerializer extends AbstractJsonSerializer<Document> {

    private void writeFragment(Fragment fragment) {
        fragment.getSequences().forEach(sequence -> {
            writeArrayField(sequence.getName(), () -> {
                writeObject(() -> {
                    writeStringField("property", fragment.getPropertyPath());
                    writeStringField("text", fragment.getText());

                    sequence.getChildren().forEach(this::writeFragment);
                });
            });
        });
    }

    @Override
    protected void serialize(Document document) throws IOException {
        writeObject(() -> {
            writeStringField("uri", document.getUri());
            writeLocalDateTimeField("created", document.getCreated());
            writeLocalDateTimeField("modified", document.getModified());

            writeFragment(document);
        });
    }
}
