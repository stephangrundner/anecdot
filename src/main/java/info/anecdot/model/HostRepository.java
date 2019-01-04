package info.anecdot.model;

import info.anecdot.model.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
@Repository
public interface HostRepository extends JpaRepository<Host, Long> {

    Host findByDirectory(Path directory);

//    @Query("select h from Host h " +
//            "where ?1 member of h.names")
//    Host findByNamesContaining(String name);

    Host findByName(String name);
}
