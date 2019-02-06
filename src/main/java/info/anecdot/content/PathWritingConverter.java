package info.anecdot.content;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
@WritingConverter
public class PathWritingConverter implements Converter<Path, String> {

    @Override
    public String convert(Path source) {
        if(source == null)
            return null;
        return source.toString();
    }
}
