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

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;

@Repository
@Secured({Role.admin, Role.releasemanager, Role.developer})
public interface ReleaseVersionRepository extends CrudRepository<ReleaseVersion, Long> {

	@Override
	@Secured({Role.user})
	Optional<ReleaseVersion> findById(Long aLong);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteById(Long aLong);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void delete(ReleaseVersion entity);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteAll(Iterable<? extends ReleaseVersion> entities);

	@Override
	@Secured({Role.admin, Role.releasemanager})
	void deleteAll();
}
