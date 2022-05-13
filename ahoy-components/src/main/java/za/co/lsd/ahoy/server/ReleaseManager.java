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

package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import za.co.lsd.ahoy.server.argocd.ApplicationNameResolver;
import za.co.lsd.ahoy.server.argocd.ArgoClient;
import za.co.lsd.ahoy.server.argocd.model.*;
import za.co.lsd.ahoy.server.clustermanager.ClusterManager;
import za.co.lsd.ahoy.server.clustermanager.ClusterManagerFactory;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.LocalRepo;
import za.co.lsd.ahoy.server.helm.ChartGenerator;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import java.util.*;

@Component
@Slf4j
public class ReleaseManager {
	private final LocalRepo localRepo;
	private final ChartGenerator chartGenerator;
	private final ArgoClient argoClient;
	private final SettingsProvider settingsProvider;
	private final ApplicationNameResolver applicationNameResolver;
	private final ClusterManagerFactory clusterManagerFactory;

	public ReleaseManager(LocalRepo localRepo, ChartGenerator chartGenerator, ArgoClient argoClient, SettingsProvider settingsProvider, ApplicationNameResolver applicationNameResolver, ClusterManagerFactory clusterManagerFactory) {
		this.localRepo = localRepo;
		this.chartGenerator = chartGenerator;
		this.argoClient = argoClient;
		this.settingsProvider = settingsProvider;
		this.applicationNameResolver = applicationNameResolver;
		this.clusterManagerFactory = clusterManagerFactory;
	}

	public ArgoApplication deploy(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, DeployOptions deployOptions) throws ReleaseManagerException {
		Objects.requireNonNull(environmentRelease, "environmentRelease is required");
		Objects.requireNonNull(releaseVersion, "releaseVersion is required");

		try {
			Optional<String> commit;
			try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
				chartGenerator.generate(environmentRelease, releaseVersion, workingTree.getPath());
				commit = workingTree.push(deployOptions.getCommitMessage());
			}

			if (commit.isPresent()) {
				localRepo.push();
			}

			Environment environment = environmentRelease.getEnvironment();
			ClusterManager clusterManager = clusterManagerFactory.newManager(environment.getCluster());
			clusterManager.createNamespace(environmentRelease.getNamespace());

			argoClient.upsertRepository();
			argoClient.createRepositoryCertificates();

			ArgoApplication argoApplication = buildApplication(environmentRelease, releaseVersion);
			String applicationName = argoApplication.getMetadata().getName();
			Optional<ArgoApplication> existingApplication = argoClient.getApplication(applicationName);
			if (existingApplication.isPresent()) {
				argoApplication = argoClient.updateApplication(argoApplication);
				argoClient.getApplication(applicationName, true); // refresh app
			} else {
				argoApplication = argoClient.createApplication(argoApplication);
			}

			log.info("Deployed with commit message: {}", deployOptions.getCommitMessage());
			return argoApplication;

		} catch (Exception e) {
			log.error("Failed to release: " + environmentRelease, e);
			throw new ReleaseManagerException("Failed to release: " + environmentRelease, e);
		}
	}

	public void undeploy(EnvironmentRelease environmentRelease) {
		Objects.requireNonNull(environmentRelease, "environmentRelease is required");

		String applicationName = environmentRelease.getArgoCdName();
		argoClient.getApplication(applicationName)
			.ifPresent((existingApplication) -> argoClient.deleteApplication(applicationName));
	}

	public Optional<ResourceTree> getResourceTree(EnvironmentRelease environmentRelease) {
		if (environmentRelease.hasCurrentReleaseVersion()) {
			return argoClient.getResourceTree(environmentRelease.getArgoCdName());
		}
		return Optional.empty();
	}

	public Optional<ArgoEvents> getEvents(EnvironmentRelease environmentRelease, String resourceUid, String resourceNamespace, String resourceName) {
		if (environmentRelease.hasCurrentReleaseVersion()) {
			return argoClient.getEvents(environmentRelease.getArgoCdName(), resourceUid, resourceNamespace, resourceName);
		}
		return Optional.empty();
	}

	public Flux<PodLog> getLogs(EnvironmentRelease environmentRelease,
								String podName,
								String resourceNamespace) {
		if (environmentRelease.hasCurrentReleaseVersion()) {
			return argoClient.getLogs(environmentRelease.getArgoCdName(), podName, resourceNamespace);
		}
		return Flux.empty();
	}

	private ArgoApplication buildApplication(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion) {
		GitSettings gitSettings = settingsProvider.getGitSettings();

		Environment environment = environmentRelease.getEnvironment();
		Release release = environmentRelease.getRelease();

		Map<String, String> labels = new HashMap<>();
		labels.put(ArgoMetadata.MANAGED_BY_LABEL, "ahoy");
		labels.put(ArgoMetadata.CLUSTER_NAME_LABEL, environment.getCluster().getName());
		labels.put(ArgoMetadata.ENVIRONMENT_NAME_LABEL, environment.getName());
		labels.put(ArgoMetadata.RELEASE_NAME_LABEL, release.getName());
		labels.put(ArgoMetadata.RELEASE_VERSION_LABEL, releaseVersion.getVersion());

		return ArgoApplication.builder()
			.metadata(ArgoMetadata.builder()
				.name(applicationNameResolver.resolve(environmentRelease))
				.labels(labels)
				.build())
			.spec(ArgoApplication.Spec.builder()
				.project("default")
				.source(ArgoApplication.Source.builder()
					.repoURL(gitSettings.getRemoteRepoUri())
					.path(ReleaseUtils.resolveReleasePath(environmentRelease))
					.targetRevision("HEAD")
					.helm(ArgoApplication.Helm.builder()
						.valueFiles(Collections.singletonList("values.yaml"))
						.build())
					.build())
				.destination(ArgoApplication.Destination.builder()
					.namespace(environmentRelease.getNamespace())
					.server(environment.getCluster().getMasterUrl())
					.build())
				.syncPolicy(ArgoSyncPolicy.builder()
					.automated(ArgoSyncPolicy.Automated.builder()
						.prune(true)
						.selfHeal(true)
						.build())
					.build())
				.build())
			.build();
	}
}