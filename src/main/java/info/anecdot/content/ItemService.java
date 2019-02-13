package info.anecdot.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    @Autowired
    private ContentService contentService;

}
