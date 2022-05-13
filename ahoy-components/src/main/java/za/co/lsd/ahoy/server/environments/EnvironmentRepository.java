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

package za.co.lsd.ahoy.server.environments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;

@Repository
@Secured({Role.admin, Role.releasemanager})
public interface EnvironmentRepository extends PagingAndSortingRepository<Environment, Long> {

	@RestResource(path = "forPromotion", rel = "forPromotion")
	@Query("select e from Environment e where e.id not in (select er.id.environmentId FROM EnvironmentRelease er where er.id.releaseId = :releaseId) order by e.orderIndex,e.id")
	Iterable<Environment> findForPromotion(@Param("releaseId") long releaseId);

	@Modifying
	@Query("update Environment e set e.orderIndex = :orderIndex where e.id = :id")
	void updateOrderIndex(@Param("id") long id, @Param("orderIndex") double orderIndex);

	@Override
	@Secured({Role.user})
	Optional<Environment> findById(Long aLong);

	@Override
	@Secured({Role.user})
	Iterable<Environment> findAll(Sort sort);

	@Override
	@Secured({Role.user})
	Page<Environment> findAll(Pageable pageable);

	@Override
	@Secured({Role.user})
	Iterable<Environment> findAll();

	@Override
	@Secured({Role.user})
	Iterable<Environment> findAllById(Iterable<Long> longs);
}
