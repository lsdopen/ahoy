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

package za.co.lsd.ahoy.server.clustermanager;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environments.Environment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Scope("prototype")
@Slf4j
public class KubernetesClusterManager implements ClusterManager {
	private final Config config;

	public KubernetesClusterManager(Cluster cluster) {
		config = new ConfigBuilder()
			.withMasterUrl(cluster.getMasterUrl())
			.withOauthToken(cluster.getToken())
			.withCaCertData(Base64.getEncoder().encodeToString(cluster.getCaCertData().getBytes(StandardCharsets.US_ASCII)))
			.build();
	}

	@Override
	public void createEnvironment(Environment environment) {
		log.debug("Creating namespace for environment {}", environment);

		try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config)) {

			kubernetesClient.namespaces().createOrReplaceWithNew()
				.withNewMetadata()
				.withName(environment.getName())
				.endMetadata()
				.done();

			log.debug("Namespace created for environment {}", environment);

		} catch (Throwable e) {
			log.error("Failed to create environment " + environment, e);
			throw new ClusterManagerException("Failed to create environment", e);
		}
	}

	@Override
	public void deleteEnvironment(Environment environment) {
		log.debug("Deleting namespace for environment {}", environment);

		try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config)) {

			if (kubernetesClient.namespaces()
				.withName(environment.getName())
				.delete()) {
				log.debug("Namespace deleted for environment {}", environment);
			}

		} catch (Throwable e) {
			log.error("Failed to delete environment " + environment, e);
			throw new ClusterManagerException("Failed to delete environment", e);
		}
	}
}
