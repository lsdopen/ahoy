/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package za.co.lsd.ahoy.server.applications;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

@Repository
@Secured({Role.admin, Role.releasemanager, Role.developer})
public interface ApplicationEnvironmentConfigRepository extends CrudRepository<ApplicationEnvironmentConfig, ApplicationDeploymentId> {

	@RestResource(path = "exists", rel = "exists")
	@Query("select CASE WHEN COUNT(c) > 0 THEN true ELSE false END from ApplicationEnvironmentConfig c where c.id.environmentReleaseId.environmentId = :environmentId and c.id.environmentReleaseId.releaseId = :releaseId and c.id.releaseVersionId = :releaseVersionId and c.id.applicationVersionId = :applicationVersionId")
	boolean exists(@Param("environmentId") long environmentId,
				   @Param("releaseId") long releaseId,
				   @Param("releaseVersionId") long releaseVersionId,
				   @Param("applicationVersionId") long applicationVersionId);

	@RestResource(path = "existingConfigs", rel = "existingConfigs")
	@Query("select c from ApplicationEnvironmentConfig c where c.id.environmentReleaseId.environmentId = :environmentId and c.id.environmentReleaseId.releaseId = :releaseId and c.id.releaseVersionId = :releaseVersionId")
	@Secured({Role.user})
	Iterable<ApplicationEnvironmentConfig> getExistingConfigs(@Param("environmentId") long environmentId,
															  @Param("releaseId") long releaseId,
															  @Param("releaseVersionId") long releaseVersionId);
}
