package za.co.lsd.ahoy.server.releases;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseVersionRepository extends CrudRepository<ReleaseVersion, Long> {
}
