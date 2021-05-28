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

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RepositoryRestController
@RequestMapping("/clusters")
@Slf4j
public class ClustersController {
	private final ClusterService clusterService;

	public ClustersController(ClusterService clusterService) {
		this.clusterService = clusterService;
	}

	@DeleteMapping("/destroy/{clusterId}")
	public ResponseEntity<?> destroy(@PathVariable Long clusterId) {
		Cluster cluster = clusterService.destroy(clusterId);

		return new ResponseEntity<>(cluster, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/test")
	public ResponseEntity<?> test(@RequestBody ClusterDTO clusterDTO) {
		clusterService.testConnection(new Cluster(clusterDTO));
		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
	}
}
