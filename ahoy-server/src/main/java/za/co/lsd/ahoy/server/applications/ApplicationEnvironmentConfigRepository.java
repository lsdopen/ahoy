package za.co.lsd.ahoy.server.applications;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationEnvironmentConfigRepository extends CrudRepository<ApplicationEnvironmentConfig, ApplicationDeploymentId> {

	@RestResource(path = "exists", rel = "exists")
	@Query("select CASE WHEN COUNT(c) > 0 THEN true ELSE false END from ApplicationEnvironmentConfig c where c.id.environmentReleaseId.environmentId = :environmentId and c.id.environmentReleaseId.releaseId = :releaseId and c.id.releaseVersionId = :releaseVersionId and c.id.applicationVersionId = :applicationVersionId")
	boolean exists(@Param("environmentId") long environmentId,
				   @Param("releaseId") long releaseId,
				   @Param("releaseVersionId") long releaseVersionId,
				   @Param("applicationVersionId") long applicationVersionId);

	@RestResource(path = "existingConfigs", rel = "existingConfigs")
	@Query("select c from ApplicationEnvironmentConfig c where c.id.environmentReleaseId.environmentId = :environmentId and c.id.environmentReleaseId.releaseId = :releaseId and c.id.releaseVersionId = :releaseVersionId")
	Iterable<ApplicationEnvironmentConfig> getExistingConfigs(@Param("environmentId") long environmentId,
															  @Param("releaseId") long releaseId,
															  @Param("releaseVersionId") long releaseVersionId);
}
