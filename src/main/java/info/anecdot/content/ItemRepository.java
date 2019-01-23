package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Item findBySiteAndUri(Site site, String uri);
    Item findBySiteAndUriAndPageIsTrue(Site site, String uri);

    List<Item> findBySiteAndUriStartingWithAndPageIsTrue(Site site, String path);
//    Slice<Page> findBySiteAndUriLike(Site site, String path, Pageable pageable);
}
