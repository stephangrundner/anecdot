package info.anecdot.content;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Stephan Grundner
 */
public interface SiteRepository extends MongoRepository<Site, String> {

    Site findByHost(String host);
}
