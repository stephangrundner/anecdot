package info.anecdot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
@Repository
public interface HostRepository extends JpaRepository<Host, Long> {

    Host findByDirectory(Path directory);

//    @Deprecated
//    Host findByName(String name);

    @Query("select h from Host h where ?1 member of h.names")
    Host findByNamesContaining(String name);
}
