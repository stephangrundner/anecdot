package info.anecdot.content;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Stephan Grundner
 */
@ReadingConverter
public class PathReadingConverter implements Converter<String, Path> {

//    @Override
//    public DBObject convert(Path source) {
//        DBObject dbObject = new BasicDBObject();
//        dbObject.put("value", source.toString());
//
//        return dbObject;
//    }

    @Override
    public Path convert(String source) {
        if(source == null)
            return null;
        return Paths.get(source);
    }
}
