package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvironmentReleaseRepository extends CrudRepository<EnvironmentRelease, EnvironmentReleaseId> {

	@RestResource(path = "byRelease", rel = "byRelease")
	Iterable<EnvironmentRelease> findByRelease_Id(@Param("releaseId") long releaseId);

	Optional<EnvironmentRelease> findByArgoCdUid(String argoCdUid);
}
