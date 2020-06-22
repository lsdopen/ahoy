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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class InitDefaultCluster {
	private final ClusterRepository clusterRepository;

	public InitDefaultCluster(ClusterRepository clusterRepository) {
		this.clusterRepository = clusterRepository;
	}

	@PostConstruct
	public void init() throws IOException {
		if (clusterRepository.count() == 0) {
			log.info("Detected no clusters setup");
			Path token = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token");
			Path caCrt = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

			if (Files.exists(token) && Files.exists(caCrt)) {
				log.info("Found token and ca.crt files");
				Cluster cluster = new Cluster("in-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
				cluster.setHost("minikube.host");
				cluster.setToken(Files.readString(token));
				cluster.setCaCertData(Files.readString(caCrt));
				cluster.setInCluster(true);
				log.info("Saving default cluster: {}", cluster);
				clusterRepository.save(cluster);
			}
		}
	}
}
