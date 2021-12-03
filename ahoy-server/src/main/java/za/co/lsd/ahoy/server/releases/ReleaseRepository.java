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

package za.co.lsd.ahoy.server.releases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;

@Repository
@Secured({Role.admin, Role.releasemanager, Role.developer})
public interface ReleaseRepository extends PagingAndSortingRepository<Release, Long> {

	@RestResource(path = "forAdd", rel = "forAdd")
	@Query("select r from Release r where r.id not in (select er.id.releaseId from EnvironmentRelease er where er.id.environmentId = :environmentId) order by r.id")
	Iterable<Release> findForAdd(@Param("environmentId") long environmentId);

	@Override
	@Secured({Role.user})
	Optional<Release> findById(Long aLong);

	@Override
	@Secured({Role.user})
	Iterable<Release> findAll(Sort sort);

	@Override
	@Secured({Role.user})
	Page<Release> findAll(Pageable pageable);

	@Override
	@Secured({Role.user})
	Iterable<Release> findAll();

	@Override
	@Secured({Role.user})
	Iterable<Release> findAllById(Iterable<Long> longs);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteById(Long aLong);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void delete(Release entity);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteAll(Iterable<? extends Release> entities);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteAll();
}
