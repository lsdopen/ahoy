package za.co.lsd.ahoy.server.environments;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends CrudRepository<Environment, Long> {

	@RestResource(path = "forPromotion", rel = "forPromotion")
	@Query("select e from Environment e where e.id not in (select er.id.environmentId FROM EnvironmentRelease er where er.id.releaseId = :releaseId)")
	Iterable<Environment> findForPromotion(@Param("releaseId") long releaseId);
}
