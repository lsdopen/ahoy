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
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.AhoyServerProperties;
import za.co.lsd.ahoy.server.security.AuthUtility;
import za.co.lsd.ahoy.server.security.Role;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class InitDefaultCluster {
	private final AhoyServerProperties serverProperties;
	private final ClusterRepository clusterRepository;

	public InitDefaultCluster(AhoyServerProperties serverProperties, ClusterRepository clusterRepository) {
		this.serverProperties = serverProperties;
		this.clusterRepository = clusterRepository;
	}

	@PostConstruct
	public void init() {
		AuthUtility.runAs(Role.admin, () -> {
			if (clusterRepository.count() == 0) {
				log.info("Detected no clusters setup, initializing default cluster..");

				Cluster cluster = new Cluster("in-cluster", "https://kubernetes.default.svc");
				cluster.setHost(serverProperties.getHost());
				cluster.setInCluster(true);
				log.info("Saving default cluster: {}", cluster);
				clusterRepository.save(cluster);
			}
		});
	}
}
