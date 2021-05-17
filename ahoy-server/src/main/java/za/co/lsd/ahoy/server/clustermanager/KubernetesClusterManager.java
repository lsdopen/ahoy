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

package za.co.lsd.ahoy.server.clustermanager;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Component
@Scope("prototype")
@Slf4j
public class KubernetesClusterManager implements ClusterManager {
	private final Cluster cluster;
	private final Config config;

	public KubernetesClusterManager(Cluster cluster) {
		this.cluster = Objects.requireNonNull(cluster, "cluster is required");

		config = new ConfigBuilder()
			.withMasterUrl(cluster.getMasterUrl())
			.withOauthToken(cluster.getToken())
			.withCaCertData(Base64.getEncoder().encodeToString(cluster.getCaCertData().getBytes(StandardCharsets.US_ASCII)))
			.build();
	}

	@Override
	public void createNamespace(String name) {
		Objects.requireNonNull(name, "name is required");

		log.debug("Creating namespace: {}", name);

		try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config)) {

			if (kubernetesClient.namespaces().withName(name).get() == null) {
				kubernetesClient.namespaces().createNew()
					.withNewMetadata()
					.withName(name)
					.endMetadata()
					.done();
				log.debug("Namespace created: {}", name);

			} else {
				log.debug("Namespace already exists: {}", name);
			}

		} catch (Throwable e) {
			log.error("Failed to create namespace: " + name, e);
			throw new ClusterManagerException("Failed to create namespace", e);
		}
	}

	@Override
	public void deleteNamespace(String name) {
		Objects.requireNonNull(name, "name is required");

		log.debug("Deleting namespace: {}", name);

		try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config)) {

			if (kubernetesClient.namespaces()
				.withName(name)
				.delete()) {
				log.debug("Namespace deleted: {}", name);
			}

		} catch (Throwable e) {
			log.error("Failed to delete namespace: " + name, e);
			throw new ClusterManagerException("Failed to delete namespace", e);
		}
	}

	@Override
	public void testConnection() {
		log.debug("Testing connection to cluster: {}", cluster);

		try (DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config)) {

			kubernetesClient.namespaces().list();
			log.debug("Connection to cluster successful: {}", cluster.getName());

		} catch (Throwable e) {
			log.error("Failed to connect to cluster: " + cluster.getName(), e);
			throw new ClusterManagerException("Failed to connect to cluster", e);
		}
	}
}
