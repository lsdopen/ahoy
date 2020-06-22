/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentService;

@Service
@Slf4j
public class ClusterService {
	private final ClusterRepository clusterRepository;
	private final EnvironmentService environmentService;

	public ClusterService(ClusterRepository clusterRepository, EnvironmentService environmentService) {
		this.clusterRepository = clusterRepository;
		this.environmentService = environmentService;
	}

	@Transactional
	public Cluster destroy(Long clusterId) {
		Cluster cluster = clusterRepository.findById(clusterId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find cluster: " + clusterId));

		clusterRepository.delete(cluster);

		log.info("Destroying cluster: {}", cluster);

		for (Environment environment : cluster.getEnvironments()) {
			environmentService.destroy(environment);
		}

		return cluster;
	}
}
