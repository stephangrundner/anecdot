package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Stephan Grundner
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    @Query("select s from Site s where ?1 member of s.hosts")
    Site findByHostsContaining(String name);
}
