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

package za.co.lsd.ahoy.server.environments;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;

@Repository
@Secured({"ROLE_admin"})
public interface EnvironmentRepository extends PagingAndSortingRepository<Environment, Long> {

	@RestResource(path = "forPromotion", rel = "forPromotion")
	@Query("select e from Environment e where e.id not in (select er.id.environmentId FROM EnvironmentRelease er where er.id.releaseId = :releaseId) order by e.orderIndex,e.id")
	Iterable<Environment> findForPromotion(@Param("releaseId") long releaseId);

	@Modifying
	@Query("update Environment e set e.orderIndex = :orderIndex where e.id = :id")
	void updateOrderIndex(@Param("id") long id, @Param("orderIndex") double orderIndex);
}
