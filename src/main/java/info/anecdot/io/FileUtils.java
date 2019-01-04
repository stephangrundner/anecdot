package info.anecdot.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Stephan Grundner
 */
public class FileUtils {

    public static BasicFileAttributes getBasicFileAttributes(Path file) {
        BasicFileAttributes attributes;
        try {
            return Files.readAttributes(file, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime getCreationTime(Path file) {
        BasicFileAttributes fileAttributes = getBasicFileAttributes(file);
        return LocalDateTime.ofInstant(fileAttributes.creationTime().toInstant(), ZoneOffset.UTC);
    }

    public static LocalDateTime getLastModifiedTime(Path file) {
        BasicFileAttributes fileAttributes = getBasicFileAttributes(file);
        return LocalDateTime.ofInstant(fileAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
    }
}
