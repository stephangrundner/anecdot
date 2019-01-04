package info.anecdot.model;

import info.anecdot.model.Payload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Stephan Grundner
 */
@NoRepositoryBean
public interface PayloadRepository<T extends Payload> extends JpaRepository<T, Long> {

}
