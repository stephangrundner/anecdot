package info.anecdot.content;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
public interface ItemRepository extends MongoRepository<Item, String> {

    Item findByUri(String uri);
    List<Item> findAllBySite(Site site);
    Item findBySiteAndUri(Site site, String uri);
    Set<Item> findAllBySiteAndTags(Site site, Collection<String> tags);
}
