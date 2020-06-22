package za.co.lsd.ahoy.server.applications;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationVersionRepository extends CrudRepository<ApplicationVersion, Long> {
}
