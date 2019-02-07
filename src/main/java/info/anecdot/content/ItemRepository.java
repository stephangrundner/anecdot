package info.anecdot.content;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
public interface ItemRepository extends MongoRepository<Item, String> {

    List<Item> findAllByHost(String host);
    Item findByHostAndUri(String host, String uri);
}
