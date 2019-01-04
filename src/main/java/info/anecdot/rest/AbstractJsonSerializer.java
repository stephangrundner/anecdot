package info.anecdot.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

/**
 * @author Stephan Grundner
 */
public abstract class AbstractJsonSerializer<T> extends JsonSerializer<T> {

    protected JsonGenerator generator;

    protected void writeObject(Runnable runnable) {
        try {
            generator.writeStartObject();
            runnable.run();
            generator.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeArray(Runnable runnable) {
        try {
            generator.writeStartArray();
            runnable.run();
            generator.writeEndArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeStringField(String fieldName, String value) {
        try {
            generator.writeStringField(fieldName, value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeLocalDateTimeField(String fieldName, LocalDateTime temporal) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            generator.writeStringField(fieldName, formatter.format(temporal));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeSomething(Runnable runnable) {
        try {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeObjectField(String fieldName, Runnable runnable) {
        try {
            generator.writeObjectFieldStart(fieldName);
            runnable.run();
            generator.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeArrayField(String fieldName, Runnable runnable) {
        try {
            generator.writeArrayFieldStart(fieldName);
            runnable.run();
            generator.writeEndArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void serialize(T t) throws IOException;

    public void serialize(T t, final JsonGenerator generator, SerializerProvider serializers) throws IOException {
        synchronized (generator) {
            this.generator = generator;

            serialize(t);
        }
    }
}
