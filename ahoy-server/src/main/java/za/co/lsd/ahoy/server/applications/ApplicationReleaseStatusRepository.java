package za.co.lsd.ahoy.server.applications;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationReleaseStatusRepository extends CrudRepository<ApplicationReleaseStatus, ApplicationDeploymentId> {

	@RestResource(path = "byReleaseVersion", rel = "byReleaseVersion")
	@Query("select s from ApplicationReleaseStatus s where s.id.environmentReleaseId.environmentId = :environmentId and s.id.environmentReleaseId.releaseId = :releaseId and s.id.releaseVersionId = :releaseVersionId")
	Iterable<ApplicationReleaseStatus> byReleaseVersion(@Param("environmentId") long environmentId,
	                                                          @Param("releaseId") long releaseId,
	                                                          @Param("releaseVersionId") long releaseVersionId);
}
