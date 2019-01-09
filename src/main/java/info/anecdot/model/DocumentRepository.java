package info.anecdot.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Document findByHostAndUri(Host host, String uri);

    List<Document> findByHostAndUriStartingWith(Host host, String path);
    Page<Document> findByHostAndUriLike(Host host, String path, Pageable pageable);
}
