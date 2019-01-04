package info.anecdot.rest;

import info.anecdot.model.Fragment;
import info.anecdot.model.Item;
import info.anecdot.model.Payload;
import info.anecdot.model.Text;

import java.io.IOException;

/**
 * @author Stephan Grundner
 */
public class ItemSerializer extends AbstractJsonSerializer<Item> {

    private void writePayload(Payload payload) {
        writeObject(() -> {
            writeStringField("property", payload.getPropertyPath());
            if (payload instanceof Text) {
                writeStringField("value", ((Text) payload).getValue());
            } else {
                writeFragment((Fragment) payload);
            }
        });
    }

    private void writeFragment(Fragment fragment) {
        fragment.getSequences().values().forEach(sequence -> {
            writeArrayField(sequence.getName(), () -> {
                sequence.getPayloads().forEach(this::writePayload);
            });
        });
    }

    @Override
    protected void serialize(Item item) throws IOException {
        writeObject(() -> {
            writeStringField("uri", item.getUri());
            writeLocalDateTimeField("created", item.getCreated());
            writeLocalDateTimeField("modified", item.getModified());

            writeFragment(item);
        });
    }
}
