package info.anecdot.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface ItemRepository extends PayloadRepository<Item> {

    Item findByHostAndUri(Host host, String uri);

    List<Item> findByHostAndUriStartingWith(Host host, String path);
    Page<Item> findByHostAndUriLike(Host host, String path, Pageable pageable);
}
