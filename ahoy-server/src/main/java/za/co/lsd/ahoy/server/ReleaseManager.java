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

package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.argocd.ApplicationNameResolver;
import za.co.lsd.ahoy.server.argocd.ArgoClient;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.argocd.model.ArgoSyncPolicy;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.LocalRepo;
import za.co.lsd.ahoy.server.helm.ChartGenerator;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class ReleaseManager {
	private final LocalRepo localRepo;
	private final ChartGenerator chartGenerator;
	private final ArgoClient argoClient;
	private final SettingsProvider settingsProvider;
	private final ApplicationNameResolver applicationNameResolver;

	public ReleaseManager(LocalRepo localRepo, ChartGenerator chartGenerator, ArgoClient argoClient, SettingsProvider settingsProvider, ApplicationNameResolver applicationNameResolver) {
		this.localRepo = localRepo;
		this.chartGenerator = chartGenerator;
		this.argoClient = argoClient;
		this.settingsProvider = settingsProvider;
		this.applicationNameResolver = applicationNameResolver;
	}

	public ArgoApplication deploy(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, DeployDetails deployDetails) throws ReleaseManagerException {
		Objects.requireNonNull(environmentRelease, "environmentRelease is required");
		Objects.requireNonNull(releaseVersion, "releaseVersion is required");

		try {
			LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree();

			chartGenerator.generate(environmentRelease, releaseVersion, workingTree.getPath());

			Optional<String> commit = workingTree.push(deployDetails.getCommitMessage());
			workingTree.delete();
			if (commit.isPresent()) {
				localRepo.push();
			}

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

			log.info("Deployed with commit message: {}", deployDetails.getCommitMessage());
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

	private ArgoApplication buildApplication(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion) {
		GitSettings gitSettings = settingsProvider.getGitSettings();

		Environment environment = environmentRelease.getEnvironment();
		return ArgoApplication.builder()
			.metadata(ArgoMetadata.builder()
				.name(applicationNameResolver.resolve(environmentRelease))
				.labels(Collections.singletonMap(ArgoMetadata.RELEASE_VERSION_LABEL, releaseVersion.getVersion()))
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
					.namespace(environment.getName())
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
