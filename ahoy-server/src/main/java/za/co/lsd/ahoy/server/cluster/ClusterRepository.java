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

package za.co.lsd.ahoy.server.cluster;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;

@Repository
@Secured({Role.admin})
public interface ClusterRepository extends PagingAndSortingRepository<Cluster, Long> {

	@Override
	@Secured({Role.user})
	long count();

	@Override
	@Secured({Role.admin, Role.releasemanager})
	Iterable<Cluster> findAll(Sort sort);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	Page<Cluster> findAll(Pageable pageable);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	Iterable<Cluster> findAll();

	@Override
	@Secured({Role.admin, Role.releasemanager})
	Iterable<Cluster> findAllById(Iterable<Long> longs);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	Optional<Cluster> findById(Long aLong);
}
