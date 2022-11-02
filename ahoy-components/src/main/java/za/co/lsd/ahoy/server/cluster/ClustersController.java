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

package za.co.lsd.ahoy.server.cluster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import za.co.lsd.ahoy.server.security.Role;

@RepositoryRestController
@RequestMapping("/clusters")
@Slf4j
@Secured({Role.admin})
public class ClustersController {
	private final ClusterService clusterService;

	public ClustersController(ClusterService clusterService) {
		this.clusterService = clusterService;
	}

	@GetMapping("/count")
	@Secured({Role.user})
	public ResponseEntity<Long> count() {
		return new ResponseEntity<>(clusterService.count(), new HttpHeaders(), HttpStatus.OK);
	}

	@DeleteMapping("/delete/{clusterId}")
	public ResponseEntity<Cluster> delete(@PathVariable Long clusterId) {
		Cluster cluster = clusterService.delete(clusterId);

		return new ResponseEntity<>(cluster, new HttpHeaders(), HttpStatus.OK);
	}
}
