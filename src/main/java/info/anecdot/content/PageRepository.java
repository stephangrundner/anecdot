package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    Page findBySiteAndUri(Site site, String uri);

    List<Page> findBySiteAndUriStartingWith(Site site, String path);
//    Slice<Page> findBySiteAndUriLike(Site site, String path, Pageable pageable);
}
