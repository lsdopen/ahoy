package za.co.lsd.ahoy.server.releases;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends CrudRepository<Release, Long> {

	@RestResource(path = "forAdd", rel = "forAdd")
	@Query("select r from Release r where r.id not in (select er.id.releaseId from EnvironmentRelease er where er.id.environmentId = :environmentId)")
	Iterable<Release> findForAdd(@Param("environmentId") long environmentId);
}
