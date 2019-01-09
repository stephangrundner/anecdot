package info.anecdot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
@Repository
public interface HostRepository extends JpaRepository<Host, Long> {

    Host findByDirectory(Path directory);
    Host findByName(String name);
}
