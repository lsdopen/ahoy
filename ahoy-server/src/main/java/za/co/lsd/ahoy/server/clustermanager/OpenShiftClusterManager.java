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
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;

import java.util.Objects;

@Component
@Scope("prototype")
@Slf4j
public class OpenShiftClusterManager implements ClusterManager {
	private final Cluster cluster;
	private final Config config;

	public OpenShiftClusterManager(Cluster cluster) {
		this.cluster = cluster;

		config = new ConfigBuilder()
			.withMasterUrl(cluster.getMasterUrl())
			.withOauthToken(cluster.getToken())
			.build();
	}

	@Override
	public void createNamespace(String name) {
		Objects.requireNonNull(name, "name is required");

		log.debug("Creating project: {}", name);

		try (DefaultOpenShiftClient openShiftClient = new DefaultOpenShiftClient(config)) {

			if (openShiftClient.projects().withName(name).get() != null) {
				openShiftClient.projects().createOrReplaceWithNew()
					.withNewMetadata()
					.withName(name)
					.endMetadata()
					.done();
				log.debug("Project created: {}", name);

			} else {
				log.debug("Project already exists: {}", name);
			}


		} catch (Throwable e) {
			log.error("Failed to create namespace: " + name, e);
			throw new ClusterManagerException("Failed to create namespace", e);
		}
	}

	@Override
	public void deleteNamespace(String name) {
		Objects.requireNonNull(name, "name is required");

		log.debug("Deleting project: {}", name);

		try (DefaultOpenShiftClient openShiftClient = new DefaultOpenShiftClient(config)) {

			if (openShiftClient.projects()
				.withName(name)
				.delete()) {
				log.debug("Project deleted: {}", name);
			}

		} catch (Throwable e) {
			log.error("Failed to delete namespace: " + name, e);
			throw new ClusterManagerException("Failed to delete namespace", e);
		}
	}

	@Override
	public void testConnection() {
		log.debug("Testing connection to cluster: {}", cluster);

		try (DefaultOpenShiftClient openShiftClient = new DefaultOpenShiftClient(config)) {

			openShiftClient.projects().list();
			log.debug("Connection to cluster successful: {}", cluster.getName());

		} catch (Throwable e) {
			log.error("Failed to connect to cluster: " + cluster.getName(), e);
			throw new ClusterManagerException("Failed to connect to cluster", e);
		}
	}
}
