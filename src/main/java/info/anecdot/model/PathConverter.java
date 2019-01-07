package info.anecdot.model;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Stephan Grundner
 */
public class PathConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path path) {
        if (path != null) {
            return path.toString();
        }

        return null;
    }

    @Override
    public Path convertToEntityAttribute(String filename) {
        if (StringUtils.hasLength(filename)) {
            return Paths.get(filename);
        }

        return null;
    }
}
