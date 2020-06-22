package za.co.lsd.ahoy.server.releases;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseHistoryRepository extends CrudRepository<ReleaseHistory, Long> {

	@RestResource(path = "findByReleaseId", rel = "findByReleaseId")
	Iterable<ReleaseHistory> findTop50ByEnvironmentReleaseReleaseIdOrderByTimeDesc(long releaseId);
}
