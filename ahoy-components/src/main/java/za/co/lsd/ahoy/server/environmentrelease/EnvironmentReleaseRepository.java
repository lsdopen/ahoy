/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;

@Repository
@Secured({Role.admin, Role.releasemanager})
public interface EnvironmentReleaseRepository extends CrudRepository<EnvironmentRelease, EnvironmentReleaseId> {

	@RestResource(path = "byRelease", rel = "byRelease")
	@Query("select e from EnvironmentRelease e where e.release.id = :releaseId order by e.environment.orderIndex,e.environment.id")
	@Secured({Role.user})
	Iterable<EnvironmentRelease> findByRelease(@Param("releaseId") long releaseId);

	Optional<EnvironmentRelease> findByArgoCdUid(String argoCdUid);

	@Override
	@Secured({Role.user})
	Iterable<EnvironmentRelease> findAll();

	@Override
	@Secured({Role.user})
	Optional<EnvironmentRelease> findById(EnvironmentReleaseId environmentReleaseId);
}
